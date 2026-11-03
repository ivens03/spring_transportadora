package br.com.fametro.spring.transportadora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configurações {@code rotacerta.*} do application.properties. */
@ConfigurationProperties("rotacerta")
public record RotaCertaProperties(Jwt jwt, Credencial credencial, Bootstrap bootstrap) {

    public record Jwt(String emissor, String segredo) {
    }

    public record Credencial(Duration validadeTokenApp,
                             Duration validadeTokenPainel,
                             Duration validadeCodigoAtivacao,
                             int maxFalhasLogin,
                             Duration tempoBloqueio) {
    }

    public record Bootstrap(Admin admin) {
        public record Admin(String nome, String email, String senha) {
        }
    }
}
