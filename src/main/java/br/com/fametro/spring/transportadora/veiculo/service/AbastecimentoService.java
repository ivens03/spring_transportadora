package br.com.fametro.spring.transportadora.veiculo.service;

import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.veiculo.dto.request.AbastecimentoRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.response.AbastecimentoResponse;
import br.com.fametro.spring.transportadora.veiculo.model.Abastecimento;
import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.repository.AbastecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Abastecimentos da frota: a matéria-prima das métricas de consumo. */
@Service
@RequiredArgsConstructor
public class AbastecimentoService {

    private final AbastecimentoRepository abastecimentoRepository;
    private final VeiculoService veiculoService;
    private final Clock clock;

    @Transactional
    public AbastecimentoResponse registrar(Long veiculoId, AbastecimentoRequest request, String solicitante) {
        Veiculo veiculo = veiculoService.carregar(veiculoId);
        if (veiculo.inativo()) {
            throw new RegraNegocioException("Veículo " + veiculo.getPlaca() + " está INATIVO: não recebe abastecimento.");
        }
        if (!veiculo.getCombustivel().aceita(request.combustivel())) {
            throw new RegraNegocioException("Veículo " + veiculo.getPlaca() + " é " + veiculo.getCombustivel()
                    + " e não pode ser abastecido com " + request.combustivel() + ".");
        }

        Instant dataHora = request.dataHora().toInstant();
        if (abastecimentoRepository.existsByVeiculoIdAndDataHora(veiculoId, dataHora)) {
            throw new ConflitoException("Já existe abastecimento deste veículo nesta data/hora.");
        }
        validarOdometro(veiculoId, dataHora, request.odometroKm());

        Instant agora = clock.instant();
        Abastecimento abastecimento = Abastecimento.novo(veiculo, dataHora, request.odometroKm(), request.litros(),
                request.valorTotal(), request.combustivel(),
                request.posto() == null || request.posto().isBlank() ? null : request.posto().trim(),
                solicitante, agora);
        abastecimentoRepository.save(abastecimento);
        veiculo.registrarOdometro(request.odometroKm(), agora);
        return AbastecimentoResponse.de(abastecimento);
    }

    @Transactional(readOnly = true)
    public Page<AbastecimentoResponse> listar(Long veiculoId, Pageable pageable) {
        veiculoService.carregar(veiculoId); // 404 se o veículo não existe
        return abastecimentoRepository.findByVeiculoId(veiculoId, pageable).map(AbastecimentoResponse::de);
    }

    @Transactional(readOnly = true)
    public AbastecimentoResponse buscar(Long veiculoId, Long abastecimentoId) {
        return abastecimentoRepository.findByIdAndVeiculoId(abastecimentoId, veiculoId)
                .map(AbastecimentoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Abastecimento " + abastecimentoId + " do veículo " + veiculoId + " não encontrado."));
    }

    /**
     * O odômetro tem que caber entre o abastecimento anterior e o seguinte (por data):
     * aceita lançamento atrasado, mas recusa km que andaria para trás.
     */
    private void validarOdometro(Long veiculoId, Instant dataHora, int odometro) {
        abastecimentoRepository.findFirstByVeiculoIdAndDataHoraLessThanOrderByDataHoraDesc(veiculoId, dataHora)
                .filter(anterior -> odometro < anterior.getOdometroKm())
                .ifPresent(anterior -> {
                    throw new RegraNegocioException("Odômetro " + odometro + " km é menor que o do abastecimento anterior ("
                            + anterior.getOdometroKm() + " km em " + anterior.getDataHora() + ").");
                });
        abastecimentoRepository.findFirstByVeiculoIdAndDataHoraGreaterThanOrderByDataHoraAsc(veiculoId, dataHora)
                .filter(seguinte -> odometro > seguinte.getOdometroKm())
                .ifPresent(seguinte -> {
                    throw new RegraNegocioException("Odômetro " + odometro + " km é maior que o do abastecimento seguinte ("
                            + seguinte.getOdometroKm() + " km em " + seguinte.getDataHora() + ").");
                });
    }
}
