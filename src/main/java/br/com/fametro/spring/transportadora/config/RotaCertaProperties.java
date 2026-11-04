package br.com.fametro.spring.transportadora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;

import java.time.Duration;
import java.util.Map;

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

    /**
     * @param admin primeiro ADMINISTRATIVO (todos os perfis)
     * @param teste contas fixas de teste, uma por perfil (só fora de prod; valores no .env)
     */
    public record Bootstrap(Admin admin, Teste teste) {
        public record Admin(String nome, String email, String senha) {
        }

        public record Teste(boolean ativo, Map<Perfil, Conta> usuarios) {
        }

        /** {@code credencial} = senha (funcionário) ou PIN (entregador). E-mail é opcional para o entregador. */
        public record Conta(String matricula, String nome, String email, String credencial) {
        }
    }
}
