package br.com.fametro.spring.transportadora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configurações {@code rotacerta.*} do application.properties (segredos vêm do .env). */
@ConfigurationProperties("rotacerta")
public record RotaCertaProperties(Jwt jwt, Credencial credencial, Seguranca seguranca, Bootstrap bootstrap) {

    public record Jwt(String emissor, String segredo) {
    }

    public record Credencial(Duration validadeTokenApp,
                             Duration validadeTokenPainel,
                             Duration validadeCodigoAtivacao,
                             int maxFalhasLogin,
                             Duration tempoBloqueio) {
    }

    /** Hash de senha/PIN: pepper (.env), validade da chave de salt e parâmetros do Argon2id. */
    public record Seguranca(String pepper, Duration validadeChaveSalt, Argon2 argon2) {
    }

    public record Argon2(int memoriaKib, int iteracoes, int paralelismo, int tamanhoHash, int tamanhoSalt) {
    }

    public record Bootstrap(Admin admin) {
        public record Admin(String nome, String email, String senha) {
        }
    }
}
