package br.com.fametro.spring.transportadora.webhook.dto.response;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.webhook.model.WebhookEntrega;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Um aviso enviado (ou a enviar) ao webhook, com o resultado da última tentativa")
public record WebhookEntregaResponse(
        @Schema(description = "Mesmo valor do header X-RotaCerta-Entrega", example = "42") Long id,
        @Schema(example = "ENTREGUE") TipoEventoEncomenda evento,
        @Schema(example = "ENTREGUE") StatusEntregaWebhook status,
        @Schema(example = "1") int tentativas,
        @Schema(example = "200") Integer ultimoStatusHttp,
        @Schema(example = "Sem resposta em 5s.") String ultimoErro,
        @Schema(description = "Quando vai tentar de novo (só PENDENTE)") Instant proximaTentativaEm,
        Instant criadaEm,
        Instant entregueEm,
        @Schema(description = "Corpo enviado") WebhookPayload payload) {

    public static WebhookEntregaResponse de(WebhookEntrega e, WebhookPayload payload) {
        return new WebhookEntregaResponse(e.getId(), e.getEvento(), e.getStatus(), e.getTentativas(),
                e.getUltimoStatusHttp(), e.getUltimoErro(), e.getProximaTentativaEm(), e.getCriadaEm(), e.getEntregueEm(),
                payload);
    }
}
