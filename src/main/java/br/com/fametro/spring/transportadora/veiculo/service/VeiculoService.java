package br.com.fametro.spring.transportadora.veiculo.service;

import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.veiculo.config.FrotaProperties;
import br.com.fametro.spring.transportadora.veiculo.dto.request.VeiculoPatchRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.request.VeiculoRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.response.VeiculoResponse;
import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import br.com.fametro.spring.transportadora.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/** Cadastro e manutenção da frota (ADMINISTRATIVO, SUPORTE e FINANCEIRO). */
@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final FrotaProperties frotaProperties;
    private final Clock clock;

    @Transactional
    public VeiculoResponse cadastrar(VeiculoRequest request, String solicitante) {
        String placa = Veiculo.normalizarPlaca(request.placa());
        String renavam = request.renavam() == null || request.renavam().isBlank() ? null : request.renavam();
        validarAno(request.anoFabricacao());
        if (veiculoRepository.existsByPlaca(placa)) {
            throw new ConflitoException("Já existe veículo com a placa " + placa + ".");
        }
        if (renavam != null && veiculoRepository.existsByRenavam(renavam)) {
            throw new ConflitoException("Já existe veículo com este RENAVAM.");
        }
        Veiculo veiculo = Veiculo.novo(placa, renavam, request.marca().trim(), request.modelo().trim(),
                request.anoFabricacao(), request.tipo(), request.combustivel(), request.capacidadeCargaKg(),
                request.capacidadeVolumeM3(), request.consumoReferenciaKmPorLitro(), request.quilometragemAtual(),
                solicitante, clock.instant());
        return VeiculoResponse.de(veiculoRepository.save(veiculo));
    }

    @Transactional(readOnly = true)
    public Page<VeiculoResponse> listar(StatusVeiculo status, TipoVeiculo tipo, Pageable pageable) {
        return veiculoRepository.findAll(VeiculoRepository.filtro(status, tipo), pageable).map(VeiculoResponse::de);
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscar(Long id) {
        return VeiculoResponse.de(carregar(id));
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoPatchRequest request) {
        if (request.vazio()) {
            throw new RegraNegocioException("Nada para atualizar: envie ao menos um campo.");
        }
        Veiculo veiculo = carregar(id);
        Instant agora = clock.instant();

        if (request.placa() != null) {
            String placa = Veiculo.normalizarPlaca(request.placa());
            if (!placa.equals(veiculo.getPlaca())) {
                if (veiculoRepository.existsByPlaca(placa)) {
                    throw new ConflitoException("Já existe veículo com a placa " + placa + ".");
                }
                veiculo.alterarPlaca(placa, agora);
            }
        }
        if (request.marca() != null) {
            veiculo.alterarMarca(obrigatorio(request.marca(), "marca"), agora);
        }
        if (request.modelo() != null) {
            veiculo.alterarModelo(obrigatorio(request.modelo(), "modelo"), agora);
        }
        if (request.capacidadeCargaKg() != null) {
            veiculo.alterarCapacidadeCargaKg(request.capacidadeCargaKg(), agora);
        }
        if (request.capacidadeVolumeM3() != null) {
            veiculo.alterarCapacidadeVolumeM3(request.capacidadeVolumeM3(), agora);
        }
        if (request.consumoReferenciaKmPorLitro() != null) {
            veiculo.alterarConsumoReferencia(request.consumoReferenciaKmPorLitro(), agora);
        }
        if (request.quilometragemAtual() != null) {
            if (request.quilometragemAtual() < veiculo.getQuilometragemAtual()) {
                throw new RegraNegocioException("A quilometragem não pode diminuir (atual: "
                        + veiculo.getQuilometragemAtual() + " km).");
            }
            veiculo.alterarQuilometragem(request.quilometragemAtual(), agora);
        }
        if (request.status() != null) {
            veiculo.alterarStatus(request.status(), agora);
        }
        return VeiculoResponse.de(veiculo);
    }

    Veiculo carregar(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo " + id + " não encontrado."));
    }

    private void validarAno(Integer ano) {
        int limite = LocalDate.now(clock.withZone(frotaProperties.fusoHorario())).getYear() + 1;
        if (ano > limite) {
            throw new RegraNegocioException("Ano de fabricação inválido: no máximo " + limite + ".");
        }
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor.isBlank()) {
            throw new RegraNegocioException("O campo " + campo + " não pode ficar em branco.");
        }
        return valor.trim();
    }
}
