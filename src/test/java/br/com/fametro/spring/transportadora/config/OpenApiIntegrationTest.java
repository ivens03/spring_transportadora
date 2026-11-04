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
                .andExpect(jsonPath("$.paths['/api/v1/auth/credenciais'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/credenciais/{matricula}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/sessao'].get.responses['401']").exists())
                // usuários (protegidos)
                .andExpect(jsonPath("$.paths['/api/v1/usuarios'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{matricula}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{matricula}'].patch.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{matricula}/codigos-ativacao'].post").exists())
                // frota
                .andExpect(jsonPath("$.paths['/api/v1/veiculos'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/veiculos'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/veiculos/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/veiculos/{veiculoId}/abastecimentos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/veiculos/{veiculoId}/abastecimentos/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/veiculos/{veiculoId}/metricas-consumo'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/metricas-consumo'].get").exists())
                // cargas e rotas
                .andExpect(jsonPath("$.paths['/api/v1/cargas'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cargas/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas/{id}/paradas/{ordem}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas/{rotaId}/posicoes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas/{rotaId}/posicoes'].get").exists())
                // portal do cliente
                .andExpect(jsonPath("$.paths['/api/v1/clientes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/encomendas'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/encomendas/{codigo}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks/{id}/segredos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks/{id}/pings'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/webhooks/{id}/entregas'].get").exists());
    }

    @Test
    void naoHaPutNaApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths[*].put").isEmpty());
    }

    @Test
    void swaggerUiEhPublico() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
