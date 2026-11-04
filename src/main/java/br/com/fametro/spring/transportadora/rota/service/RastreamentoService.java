package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.rota.config.RotaProperties;
import br.com.fametro.spring.transportadora.rota.dto.request.LotePosicoesRequest;
import br.com.fametro.spring.transportadora.rota.dto.request.PosicaoRequest;
import br.com.fametro.spring.transportadora.rota.dto.response.LotePosicoesResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.PosicaoResponse;
import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.rota.repository.PosicaoRotaLoteRepository;
import br.com.fametro.spring.transportadora.rota.repository.PosicaoRotaRepository;
import br.com.fametro.spring.transportadora.encomenda.evento.PosicoesRecebidas;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Posições de GPS da rota: o app envia em lote; o mapa do Suporte lê o rastro (incremental com {@code desde}). */
@Service
@RequiredArgsConstructor
public class RastreamentoService {

    private final RotaService rotaService;
    private final PosicaoRotaRepository posicaoRotaRepository;
    private final PosicaoRotaLoteRepository posicaoRotaLoteRepository;
    private final RotaProperties props;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    /**
     * Aceita com a rota EM_ANDAMENTO e também CONCLUIDA: o celular pode ter ficado sem sinal e
     * mandar o rastro depois de baixar a última parada.
     */
    @Transactional
    public LotePosicoesResponse registrar(Long rotaId, LotePosicoesRequest lote, Solicitante quem) {
        if (lote.posicoes().size() > props.maxPosicoesPorLote()) {
            throw new RegraNegocioException("Máximo de " + props.maxPosicoesPorLote() + " posições por envio.");
        }
        Rota rota = rotaService.carregar(rotaId, quem);
        if (rota.getStatus() != StatusRota.EM_ANDAMENTO && rota.getStatus() != StatusRota.CONCLUIDA) {
            throw new RegraNegocioException("A rota " + rota.getCodigo() + " está " + rota.getStatus()
                    + ": posições só depois de iniciar.");
        }
        Instant agora = clock.instant();
        Instant limite = agora.plus(props.toleranciaRelogio());
        if (lote.posicoes().stream().anyMatch(p -> p.registradaEm().toInstant().isAfter(limite))) {
            throw new RegraNegocioException("Há posição com hora no futuro: confira o relógio do celular.");
        }
        // mesmo instante repetido dentro do lote: fica a primeira
        Map<Instant, PosicaoRequest> unicas = new LinkedHashMap<>();
        lote.posicoes().stream()
                .sorted(Comparator.comparing(p -> p.registradaEm().toInstant()))
                .forEach(p -> unicas.putIfAbsent(p.registradaEm().toInstant(), p));

        int gravadas = posicaoRotaLoteRepository.inserirNovas(rota.getId(), List.copyOf(unicas.values()), agora);
        if (gravadas > 0 && rota.getStatus() == StatusRota.EM_ANDAMENTO) {
            eventos.publishEvent(new PosicoesRecebidas(rota.getId())); // webhook de localização
        }
        return new LotePosicoesResponse(lote.posicoes().size(), gravadas, lote.posicoes().size() - gravadas);
    }

    @Transactional(readOnly = true)
    public List<PosicaoResponse> listar(Long rotaId, Instant desde, Solicitante quem) {
        Rota rota = rotaService.carregar(rotaId, quem);
        return posicaoRotaRepository
                .findByRotaIdAndRegistradaEmAfterOrderByRegistradaEmAsc(rota.getId(), desde == null ? Instant.EPOCH : desde)
                .stream().map(PosicaoResponse::de).toList();
    }
}
