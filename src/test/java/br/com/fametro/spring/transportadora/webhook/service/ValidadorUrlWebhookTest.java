package br.com.fametro.spring.transportadora.webhook.service;

import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.webhook.config.WebhookProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de produção (padrão): só https e nunca rede interna. Sem DNS: só IPs literais. */
class ValidadorUrlWebhookTest {

    private final ValidadorUrlWebhook prod = new ValidadorUrlWebhook(new WebhookProperties(true, false, 5,
            Duration.ofSeconds(60), 8, Duration.ofSeconds(30), Duration.ofHours(1), Duration.ofSeconds(3),
            Duration.ofSeconds(5), 20, Duration.ofMinutes(2)));

    @Test
    void aceitaHttpsPublico() {
        assertThat(prod.validar(" https://8.8.8.8/avisos ").toString()).isEqualTo("https://8.8.8.8/avisos");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://8.8.8.8/avisos",            // sem https
            "ftp://8.8.8.8/avisos",             // esquema estranho
            "https://127.0.0.1/avisos",         // loopback
            "https://10.1.2.3/avisos",          // privado
            "https://172.25.136.165/avisos",    // privado
            "https://192.168.0.10/avisos",      // privado
            "https://169.254.169.254/latest",   // metadados da nuvem
            "https://100.64.0.1/avisos",        // CGNAT
            "https://[::1]/avisos",             // loopback IPv6
            "https://[fd00::1]/avisos",         // ULA IPv6
            "https://0.0.0.0/avisos",
            "https://usuario:senha@8.8.8.8/avisos",
            "https:///sem-host",
            "nem uma url"})
    void recusa(String url) {
        assertThatThrownBy(() -> prod.validar(url)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void assinaturaHmacSha256() {
        // vetor conferido com: printf '1700000000.{"a":1}' | openssl dgst -sha256 -hmac segredo
        assertThat(AssinaturaWebhook.assinar("segredo", "1700000000", "{\"a\":1}"))
                .isEqualTo("sha256=41634607aa61528682a37f94a43a64ca2d3a6a30bc8f2fb88f8f754286d90e85");
        assertThat(AssinaturaWebhook.gerarSegredo()).startsWith("whsec_").hasSize(6 + 43)
                .isNotEqualTo(AssinaturaWebhook.gerarSegredo());
    }
}
