package br.com.fametro.spring.transportadora.webhook.config;

import br.com.fametro.spring.transportadora.webhook.service.DespachanteWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Roda o despachante de webhooks de tempos em tempos ({@code rotacerta.webhook.intervalo-despacho}, ISO-8601).
 * Desligável com {@code rotacerta.webhook.despacho-automatico=false} (os testes chamam o despachante na mão).
 */
@Configuration
@EnableScheduling
@ConditionalOnBooleanProperty(name = "rotacerta.webhook.despacho-automatico", matchIfMissing = true)
@RequiredArgsConstructor
class WebhookAgendamentoConfig {

    private final DespachanteWebhook despachante;

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "${rotacerta.webhook.intervalo-despacho:PT2S}")
    void despachar() {
        despachante.despacharPendentes();
    }
}
