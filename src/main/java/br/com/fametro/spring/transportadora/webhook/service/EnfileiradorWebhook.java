package br.com.fametro.spring.transportadora.webhook.service;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResumoResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.RastreioResponse;
import br.com.fametro.spring.transportadora.encomenda.evento.EncomendaAlterada;
import br.com.fametro.spring.transportadora.encomenda.evento.PosicoesRecebidas;
import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.encomenda.service.RastreioEncomendaService;
import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.repository.RotaRepository;
import br.com.fametro.spring.transportadora.webhook.config.WebhookProperties;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookPayload;
import br.com.fametro.spring.transportadora.webhook.model.Webhook;
import br.com.fametro.spring.transportadora.webhook.model.WebhookEntrega;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import br.com.fametro.spring.transportadora.webhook.repository.WebhookEntregaRepository;
import br.com.fametro.spring.transportadora.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transforma os eventos da encomenda em avisos na fila (outbox). Roda dentro da transação de quem publicou:
 * se a mudança for desfeita (rollback), o aviso some junto — nunca se avisa algo que não aconteceu.
 */
@Component
@RequiredArgsConstructor
class EnfileiradorWebhook {

    private final WebhookRepository webhookRepository;
    private final WebhookEntregaRepository entregaRepository;
    private final RotaRepository rotaRepository;
    private final RastreioEncomendaService rastreioService;
    private final WebhookProperties props;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @EventListener
    void aoAlterar(EncomendaAlterada e) {
        Carga carga = e.carga();
        if (carga.getCliente() == null) {
            return;
        }
        List<Webhook> destinos = webhookRepository.findByClienteIdAndAtivoTrue(carga.getCliente().getId()).stream()
                .filter(w -> w.recebe(e.tipo())).toList();
        if (destinos.isEmpty()) {
            return;
        }
        String payload = json(e.tipo(), e.ocorridoEm(), carga, rastreioService.rastrear(carga).orElse(null), e.detalhe());
        Instant agora = clock.instant();
        destinos.forEach(w -> entregaRepository.save(WebhookEntrega.nova(w, carga.getId(), e.tipo(), payload, agora)));
    }

    /** Localização: para cada encomenda ainda pendente da rota, no máximo um aviso a cada intervalo. */
    @EventListener
    void aoReceberPosicoes(PosicoesRecebidas e) {
        Rota rota = rotaRepository.findById(e.rotaId()).orElse(null);
        if (rota == null) {
            return;
        }
        Optional<PosicaoRota> ultima = rastreioService.ultimaPosicao(rota);
        if (ultima.isEmpty()) {
            return;
        }
        Instant agora = clock.instant();
        Instant limite = agora.minus(props.intervaloLocalizacao());
        Map<Long, List<Webhook>> porCliente = new HashMap<>();
        TipoEventoEncomenda tipo = TipoEventoEncomenda.LOCALIZACAO;

        for (ParadaRota parada : rota.getParadas()) {
            Carga carga = parada.getCarga();
            if (!parada.pendente() || carga.getCliente() == null) {
                continue;
            }
            List<Webhook> destinos = porCliente.computeIfAbsent(carga.getCliente().getId(), id ->
                    webhookRepository.findByClienteIdAndAtivoTrue(id).stream().filter(w -> w.recebe(tipo)).toList());
            String payload = null;
            for (Webhook w : destinos) {
                Instant anterior = entregaRepository.ultimoCriado(w.getId(), carga.getId(), tipo);
                if (anterior != null && anterior.isAfter(limite)) {
                    continue; // avisou há pouco
                }
                if (payload == null) {
                    RastreioResponse rastreio = rastreioService.rastrear(parada, ultima);
                    // detalhe = frase pronta com a rua e quanto falta (o JSON também traz os campos separados)
                    payload = json(tipo, ultima.get().getRegistradaEm(), carga, rastreio, RastreioEncomendaService.resumo(rastreio));
                }
                entregaRepository.descartarPendentes(w.getId(), carga.getId(), tipo,
                        StatusEntregaWebhook.PENDENTE, StatusEntregaWebhook.DESCARTADA);
                entregaRepository.save(WebhookEntrega.nova(w, carga.getId(), tipo, payload, agora));
            }
        }
    }

    /** PING pedido pelo cliente para testar o endereço. */
    WebhookEntrega ping(Webhook webhook) {
        Instant agora = clock.instant();
        String payload = json(TipoEventoEncomenda.PING, agora, null, null, "Teste do webhook " + webhook.getId() + ".");
        return entregaRepository.save(WebhookEntrega.nova(webhook, null, TipoEventoEncomenda.PING, payload, agora));
    }

    private String json(TipoEventoEncomenda tipo, Instant ocorridoEm, Carga carga, RastreioResponse rastreio, String detalhe) {
        return jsonMapper.writeValueAsString(new WebhookPayload(tipo, tipo.getDescricao(), ocorridoEm,
                carga == null ? null : EncomendaResumoResponse.de(carga), rastreio, detalhe));
    }
}
