package br.com.fametro.spring.transportadora.webhook.dto.response;

import br.com.fametro.spring.transportadora.webhook.model.Webhook;
import io.swagger.v3.oas.annotations.media.Schema;

/** Única vez em que o segredo inteiro aparece: guarde-o, ele não é mostrado de novo. */
@Schema(description = "Webhook + segredo completo (mostrado só agora)")
public record WebhookSegredoResponse(
        WebhookResponse webhook,
        @Schema(example = "whsec_q0Zx2...") String segredo,
        @Schema(example = "Assinatura: X-RotaCerta-Assinatura = sha256=HMAC-SHA256(segredo, X-RotaCerta-Timestamp + \".\" + corpo)")
        String comoValidar) {

    public static WebhookSegredoResponse de(Webhook w) {
        return new WebhookSegredoResponse(WebhookResponse.de(w), w.getSegredo(),
                "Recalcule HMAC-SHA256 com este segredo sobre X-RotaCerta-Timestamp + \".\" + corpo bruto, em hexadecimal, "
                        + "e compare (em tempo constante) com X-RotaCerta-Assinatura (sem o prefixo sha256=). "
                        + "Recuse se o timestamp tiver mais de 5 minutos. Responda 2xx rápido; senão o aviso é reenviado.");
    }
}
