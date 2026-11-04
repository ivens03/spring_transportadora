package br.com.fametro.spring.transportadora.usuario.config;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties.Bootstrap.Conta;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Contas FIXAS de teste, uma por perfil (matrícula, e-mail e senha/PIN no .env).
 * Só existe fora de prod ({@code @Profile("!prod")}) e com {@code rotacerta.bootstrap.teste.ativo=true}.
 * A cada subida a conta volta a ficar pronta: credencial do .env, ativa, destravada e com chave de salt válida,
 * então os 30 dias de validade nunca atrapalham o teste.
 */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnBooleanProperty("rotacerta.bootstrap.teste.ativo")
@Order(2) // depois do administrativo inicial
@RequiredArgsConstructor
public class ContasTesteConfig implements ApplicationRunner {

    private final UsuarioService usuarioService;
    private final RotaCertaProperties props;

    @Override
    public void run(ApplicationArguments args) {
        Map<Perfil, Conta> contas = props.bootstrap().teste().usuarios();
        if (contas == null || contas.isEmpty()) {
            log.warn("rotacerta.bootstrap.teste.ativo=true, mas nenhuma conta de teste configurada no .env.");
            return;
        }
        for (Perfil perfil : Perfil.values()) {
            Conta c = contas.get(perfil);
            if (c == null || c.matricula() == null || c.matricula().isBlank()
                    || c.credencial() == null || c.credencial().isBlank()) {
                log.warn("Conta de teste {} sem matrícula/credencial no .env: ignorada.", perfil);
                continue;
            }
            String nome = c.nome() == null || c.nome().isBlank() ? "Teste " + perfil : c.nome();
            var situacao = usuarioService.garantirContaTeste(perfil, c.matricula(), nome, c.email(), c.credencial());
            log.info("Conta de teste {} {} ({}): {}", perfil, c.matricula(),
                    c.email() == null || c.email().isBlank() ? "app" : c.email(), situacao);
        }
    }
}
