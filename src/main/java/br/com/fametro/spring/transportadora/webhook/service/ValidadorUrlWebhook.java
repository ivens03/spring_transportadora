package br.com.fametro.spring.transportadora.webhook.service;

import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.webhook.config.WebhookProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Valida a URL do webhook no cadastro e de novo a cada envio (o DNS pode mudar depois do cadastro).
 * Em prod: só https e nunca IP interno — senão qualquer cliente faria a API chamar a rede interna (SSRF).
 */
@Component
@RequiredArgsConstructor
public class ValidadorUrlWebhook {

    private final WebhookProperties props;

    public URI validar(String url) {
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new RegraNegocioException("URL do webhook inválida.");
        }
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("https") && !esquema.equals("http")) {
            throw new RegraNegocioException("A URL do webhook precisa começar com https://.");
        }
        if (props.exigirHttps() && !esquema.equals("https")) {
            throw new RegraNegocioException("A URL do webhook precisa ser https.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new RegraNegocioException("A URL do webhook não tem servidor (host).");
        }
        if (uri.getUserInfo() != null) {
            throw new RegraNegocioException("Não coloque usuário/senha na URL do webhook: use o segredo para validar.");
        }
        if (uri.getFragment() != null) {
            throw new RegraNegocioException("A URL do webhook não pode ter #fragmento.");
        }
        if (!props.permitirRedeInterna()) {
            InetAddress[] enderecos;
            try {
                enderecos = InetAddress.getAllByName(uri.getHost());
            } catch (UnknownHostException e) {
                throw new RegraNegocioException("Servidor do webhook não encontrado: " + uri.getHost() + ".");
            }
            for (InetAddress ip : enderecos) {
                if (interno(ip)) {
                    throw new RegraNegocioException("A URL do webhook aponta para a rede interna (" + ip.getHostAddress()
                            + "): use um endereço público.");
                }
            }
        }
        return uri;
    }

    /** Loopback, privado (10/8, 172.16/12, 192.168/16), link-local (169.254 = metadados de nuvem), CGNAT, ULA IPv6. */
    static boolean interno(InetAddress ip) {
        if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress()
                || ip.isMulticastAddress()) {
            return true;
        }
        byte[] b = ip.getAddress();
        if (ip instanceof Inet6Address) {
            return (b[0] & 0xfe) == 0xfc; // fc00::/7
        }
        return (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 64; // 100.64.0.0/10
    }
}
