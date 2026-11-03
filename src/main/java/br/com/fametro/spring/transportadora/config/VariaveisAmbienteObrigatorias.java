package br.com.fametro.spring.transportadora.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.List;

/**
 * Falha rápido, com mensagem clara, se faltar alguma variável obrigatória
 * (no dev elas vêm do .env; no prod, de variáveis de ambiente do servidor).
 * Roda antes de tudo (até do banner), por isso é registrada direto no {@code main}.
 */
public class VariaveisAmbienteObrigatorias implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final List<String> OBRIGATORIAS = List.of(
            "POSTGRES_HOST", "POSTGRES_PORT", "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD",
            "ROTACERTA_JWT_SEGREDO", "ROTACERTA_PEPPER", "ROTACERTA_ADMIN_EMAIL");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        List<String> faltando = OBRIGATORIAS.stream()
                .filter(chave -> env.getProperty(chave, "").isBlank())
                .toList();
        if (faltando.isEmpty()) {
            return;
        }
        String[] perfis = env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : env.getDefaultProfiles();
        throw new IllegalStateException("""

                Variáveis obrigatórias não definidas (perfil %s): %s
                  - dev : preencha o .env na raiz do projeto (cp .env.example .env)
                  - prod: defina como variáveis de ambiente do servidor
                """.formatted(Arrays.toString(perfis), String.join(", ", faltando)));
    }
}
