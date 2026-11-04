package br.com.fametro.spring.transportadora.webhook.dto.response;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.webhook.model.Webhook;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Webhook do cliente (o segredo só aparece na criação e na troca de segredo)")
public record WebhookResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "https://minhaloja.com.br/rotacerta/avisos") String url,
        Set<TipoEventoEncomenda> eventos,
        boolean ativo,
        @Schema(description = "Início do segredo, para conferir qual está em uso", example = "whsec_Ab3x…") String segredoInicio,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static WebhookResponse de(Webhook w) {
        return new WebhookResponse(w.getId(), w.getUrl(), w.getEventos(), w.isAtivo(),
                w.getSegredo().substring(0, Math.min(10, w.getSegredo().length())) + "…", w.getCriadoEm(), w.getAtualizadoEm());
    }
}
