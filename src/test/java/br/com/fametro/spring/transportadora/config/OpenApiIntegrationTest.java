package br.com.fametro.spring.transportadora.config;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Swagger/OpenAPI: público, documenta todos os endpoints e o esquema de segurança Bearer. */
class OpenApiIntegrationTest extends IntegracaoTestBase {

    @Test
    void documentacaoEhPublicaECobreTodosOsEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value(containsString("RotaCerta API")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.ErroResponse").exists())
                // autenticação (públicos: sem security)
                .andExpect(jsonPath("$.paths['/api/v1/auth/app/tokens'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/auth/painel/tokens'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/ativacoes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/app/credencial'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/painel/credencial'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/sessao'].get.responses['401']").exists())
                // usuários (protegidos)
                .andExpect(jsonPath("$.paths['/api/v1/painel/gestao/usuarios'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/painel/gestao/usuarios'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/painel/gestao/usuarios/{matricula}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/painel/gestao/usuarios/{matricula}/codigos-ativacao'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/painel/gestao/usuarios/{matricula}/bloqueio'].put").exists());
    }

    @Test
    void swaggerUiEhPublico() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
