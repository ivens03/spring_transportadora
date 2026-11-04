package br.com.fametro.spring.transportadora.webhook.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Assinatura dos webhooks: {@code X-RotaCerta-Assinatura: sha256=<hex>} com
 * HMAC-SHA256(segredo, timestamp + "." + corpo). O cliente recalcula com o segredo dele e compara;
 * o timestamp no cálculo impede reaproveitar um aviso antigo (o cliente recusa se estiver velho, ex.: > 5 min).
 */
public final class AssinaturaWebhook {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private AssinaturaWebhook() {
    }

    public static String assinar(String segredo, String timestamp, String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((timestamp + "." + corpo).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível na JVM", e);
        }
    }

    /** whsec_ + 32 bytes aleatórios em base64url (256 bits). */
    public static String gerarSegredo() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
