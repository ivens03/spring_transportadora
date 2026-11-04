package br.com.fametro.spring.transportadora.webhook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class WebhookConfig {

    /** Cliente HTTP dos webhooks: nunca segue redirecionamento (um 302 para a rede interna burlaria a validação). */
    @Bean
    public HttpClient webhookHttpClient(WebhookProperties props) {
        return HttpClient.newBuilder()
                .connectTimeout(props.timeoutConexao())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
