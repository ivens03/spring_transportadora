package br.com.fametro.spring.transportadora.monitoramento;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArquiteturaControllerIntegrationTest extends IntegracaoTestBase {

    private static final String URL = "/api/v1/arquitetura";

    @Test
    void desenvolvimentoVeOMapaEOAmbienteAoVivo() throws Exception {
        acessar(tokenFuncionario("DESENVOLVIMENTO"), URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.camadas[*].id").value(hasItems("celular", "navegador", "borda", "api", "banco", "vps")))
                .andExpect(jsonPath("$.fluxos[*].id").value(hasItems("login", "entrega", "gps", "deploy")))
                .andExpect(jsonPath("$.ferramentas.length()").value(greaterThan(5)))
                // medido no servidor: Postgres do Testcontainers e a última migração do Flyway
                .andExpect(jsonPath("$.ambiente.banco").value(startsWith("PostgreSQL ")))
                .andExpect(jsonPath("$.ambiente.migracao").value(startsWith("6 ")))
                .andExpect(jsonPath("$.ambiente.springBoot").value(startsWith("4.")))
                .andExpect(jsonPath("$.ambiente.java").value(startsWith("25")));
    }

    @Test
    void soQuemMonitora() throws Exception {
        acessar(tokenFuncionario("SUPORTE"), URL).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
