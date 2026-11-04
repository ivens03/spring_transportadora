package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.repository.CargaRepository;
import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.rota.config.RotaProperties;
import br.com.fametro.spring.transportadora.rota.dto.request.ParadaPatchRequest;
import br.com.fametro.spring.transportadora.rota.dto.request.RotaPatchRequest;
import br.com.fametro.spring.transportadora.rota.dto.request.RotaRequest;
import br.com.fametro.spring.transportadora.rota.dto.response.RotaResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.RotaResumoResponse;
import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.rota.repository.RotaRepository;
import br.com.fametro.spring.transportadora.rota.service.Geo.Ponto;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Roteirização: o SUPORTE escolhe cargas + veículo + entregador e o sistema gera a rota otimizada;
 * o entregador inicia a rota no app e baixa as paradas; a rota conclui sozinha na última baixa.
 */
@Service
@RequiredArgsConstructor
public class RotaService {

    private static final Set<StatusRota> ATIVAS = Set.of(StatusRota.PLANEJADA, StatusRota.EM_ANDAMENTO);

    private final RotaRepository rotaRepository;
    private final CargaRepository cargaRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RotaMapper rotaMapper;
    private final RotaProperties props;
    private final Clock clock;

    @Transactional
    public RotaResponse gerar(RotaRequest req, String solicitante) {
        Set<Long> ids = new LinkedHashSet<>(req.cargaIds());
        if (ids.size() != req.cargaIds().size()) {
            throw new RegraNegocioException("A mesma carga foi enviada mais de uma vez.");
        }
        if (ids.size() > props.maxParadas()) {
            throw new RegraNegocioException("Máximo de " + props.maxParadas() + " cargas por rota.");
        }
        if ((req.origemLatitude() == null) != (req.origemLongitude() == null)) {
            throw new RegraNegocioException("Informe origemLatitude e origemLongitude juntas (ou nenhuma, para sair do depósito).");
        }

        Veiculo veiculo = veiculoRepository.findById(req.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo " + req.veiculoId() + " não encontrado."));
        if (veiculo.getStatus() != StatusVeiculo.ATIVO) {
            throw new RegraNegocioException("Veículo " + veiculo.getPlaca() + " está " + veiculo.getStatus() + ".");
        }
        if (rotaRepository.existsByVeiculoIdAndStatusIn(veiculo.getId(), ATIVAS)) {
            throw new ConflitoException("Veículo " + veiculo.getPlaca() + " já está numa rota PLANEJADA ou EM_ANDAMENTO.");
        }

        String matricula = Usuario.normalizarMatricula(req.entregadorMatricula());
        Usuario entregador = usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador " + matricula + " não encontrado."));
        if (entregador.getPerfil() != Perfil.ENTREGADOR) {
            throw new RegraNegocioException(matricula + " não é entregador (perfil " + entregador.getPerfil() + ").");
        }
        if (entregador.getStatus() != StatusUsuario.ATIVO) {
            throw new RegraNegocioException("Entregador " + matricula + " está " + entregador.getStatus() + ".");
        }
        if (rotaRepository.existsByEntregadorIdAndStatusIn(entregador.getId(), ATIVAS)) {
            throw new ConflitoException("Entregador " + matricula + " já está numa rota PLANEJADA ou EM_ANDAMENTO.");
        }

        List<Carga> cargas = cargaRepository.travarPorIds(ids);
        if (cargas.size() != ids.size()) {
            Set<Long> faltando = new HashSet<>(ids);
            cargas.forEach(c -> faltando.remove(c.getId()));
            throw new RecursoNaoEncontradoException("Cargas não encontradas: " + faltando + ".");
        }
        List<String> indisponiveis = cargas.stream().filter(c -> !c.disponivelParaRota())
                .map(c -> c.getCodigo() + " (" + c.getStatus() + ")").toList();
        if (!indisponiveis.isEmpty()) {
            throw new RegraNegocioException("Cargas fora de AGUARDANDO_ROTA: " + String.join(", ", indisponiveis) + ".");
        }

        BigDecimal peso = cargas.stream().map(Carga::getPesoKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal volume = cargas.stream().map(Carga::getVolumeM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (peso.compareTo(veiculo.getCapacidadeCargaKg()) > 0) {
            throw new RegraNegocioException("Peso total " + peso + " kg passa da capacidade do veículo "
                    + veiculo.getPlaca() + " (" + veiculo.getCapacidadeCargaKg() + " kg).");
        }
        if (volume.compareTo(veiculo.getCapacidadeVolumeM3()) > 0) {
            throw new RegraNegocioException("Volume total " + volume + " m³ passa da capacidade do veículo "
                    + veiculo.getPlaca() + " (" + veiculo.getCapacidadeVolumeM3() + " m³).");
        }

        // ordem otimizada das paradas
        BigDecimal origemLat = req.origemLatitude() != null ? req.origemLatitude() : BigDecimal.valueOf(props.origem().latitude());
        BigDecimal origemLng = req.origemLongitude() != null ? req.origemLongitude() : BigDecimal.valueOf(props.origem().longitude());
        boolean retornar = req.retornarAOrigem() == null || req.retornarAOrigem();
        List<Ponto> destinos = cargas.stream().map(c -> RotaMapper.ponto(c.getLatitude(), c.getLongitude())).toList();
        OtimizadorRota.Plano plano = OtimizadorRota.otimizar(RotaMapper.ponto(origemLat, origemLng), destinos, retornar);

        double kmRuas = plano.totalKm() * props.fatorDistanciaViaria();
        int minutos = (int) Math.ceil(kmRuas / props.velocidadeMediaKmH() * 60) + cargas.size() * props.minutosPorParada();

        Instant agora = clock.instant();
        Rota rota = Rota.nova("ROT-%06d".formatted(rotaRepository.proximoNumeroCodigo()), veiculo, entregador,
                origemLat, origemLng, retornar, RotaMapper.km(kmRuas), minutos, peso, volume, solicitante, agora);
        for (int i = 0; i < plano.ordem().size(); i++) {
            Carga carga = cargas.get(plano.ordem().get(i));
            rota.adicionarParada(carga, i + 1, RotaMapper.km(plano.distanciasKm().get(i) * props.fatorDistanciaViaria()));
            carga.roteirizar(agora);
        }
        return rotaMapper.completa(rotaRepository.save(rota));
    }

    @Transactional(readOnly = true)
    public Page<RotaResumoResponse> listar(StatusRota status, Solicitante quem, Pageable pageable) {
        String soDoEntregador = quem.entregador() ? quem.matricula() : null;
        return rotaRepository.findAll(RotaRepository.filtro(status, soDoEntregador), pageable).map(RotaResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public RotaResponse buscar(Long id, Solicitante quem) {
        return rotaMapper.completa(carregar(id, quem));
    }

    /** Entregador: EM_ANDAMENTO (iniciar). Painel: CANCELADA. Repetir o status atual é inofensivo. */
    @Transactional
    public RotaResponse atualizar(Long id, RotaPatchRequest req, Solicitante quem) {
        Rota rota = carregar(id, quem);
        if (rota.getStatus() == req.status()) {
            return rotaMapper.completa(rota); // reenvio do app
        }
        Instant agora = clock.instant();
        switch (req.status()) {
            case EM_ANDAMENTO -> {
                if (!quem.entregador()) {
                    throw new RegraNegocioException("Só o entregador inicia a rota (pelo app).");
                }
                rota.iniciar(agora);
            }
            case CANCELADA -> {
                if (quem.entregador()) {
                    throw new RegraNegocioException("O entregador não cancela rota: fale com o Suporte.");
                }
                rota.cancelar(agora);
            }
            default -> throw new RegraNegocioException("status aceita EM_ANDAMENTO (iniciar) ou CANCELADA; "
                    + "CONCLUIDA é automática quando a última parada é baixada.");
        }
        return rotaMapper.completa(rota);
    }

    /** Baixa de parada pelo entregador (ENTREGUE ou NAO_ENTREGUE com motivo). */
    @Transactional
    public RotaResponse baixarParada(Long id, int ordem, ParadaPatchRequest req, Solicitante quem) {
        Rota rota = carregar(id, quem);
        ParadaRota parada = rota.parada(ordem);
        if (req.status() == StatusParada.PENDENTE) {
            throw new RegraNegocioException("status aceita ENTREGUE ou NAO_ENTREGUE.");
        }
        String motivo = req.motivo() == null || req.motivo().isBlank() ? null : req.motivo().trim();
        if (req.status() == StatusParada.NAO_ENTREGUE && motivo == null) {
            throw new RegraNegocioException("Informe o motivo da não entrega.");
        }
        if (!parada.pendente()) {
            if (parada.getStatus() == req.status() && Objects.equals(parada.getMotivo(), motivo)) {
                return rotaMapper.completa(rota); // reenvio do app
            }
            throw new ConflitoException("A parada " + ordem + " já foi baixada como " + parada.getStatus() + ".");
        }
        Instant agora = clock.instant();
        Instant quando = req.finalizadaEm() == null ? agora : req.finalizadaEm().toInstant();
        if (quando.isAfter(agora.plus(props.toleranciaRelogio()))) {
            throw new RegraNegocioException("finalizadaEm está no futuro.");
        }
        rota.baixarParada(parada, req.status(), motivo, quando, agora);
        return rotaMapper.completa(rota);
    }

    /** Rota de outro entregador responde 404 (não revela que existe). */
    Rota carregar(Long id, Solicitante quem) {
        return rotaRepository.findById(id)
                .filter(r -> !quem.entregador() || r.pertenceA(quem.matricula()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rota " + id + " não encontrada."));
    }
}
