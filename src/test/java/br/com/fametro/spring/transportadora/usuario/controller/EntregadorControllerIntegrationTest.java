package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/entregadores: o Suporte escolhe o entregador da rota sem acessar /usuarios. */
class EntregadorControllerIntegrationTest extends IntegracaoTestBase {

    private static final String ENTREGADORES = "/api/v1/entregadores";

    @Test
    void suporteListaSoEntregadoresAtivos() throws Exception {
        EntregadorTeste ativo = entregadorAtivo();
        Criado pendente = criar(tokenAdmin(), entregador());
        String suporte = tokenFuncionario("SUPORTE");

        acessar(suporte, ENTREGADORES + "?status=ATIVO&size=100&sort=criadoEm,desc")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].matricula", everyItem(startsWith("ENT-"))))
                .andExpect(jsonPath("$.content[*].matricula", hasItem(ativo.matricula())))
                .andExpect(jsonPath("$.content[*].matricula", not(hasItem(pendente.matricula()))))
                .andExpect(jsonPath("$.content[*].status", everyItem(org.hamcrest.Matchers.is("ATIVO"))))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").isNumber());
        // o suporte continua sem acesso ao cadastro completo
        acessar(suporte, USUARIOS).andExpect(status().isForbidden());
    }

    @Test
    void administrativoTambemListaEDemaisNao() throws Exception {
        acessar(tokenAdmin(), ENTREGADORES).andExpect(status().isOk());
        acessar(tokenFuncionario("FINANCEIRO"), ENTREGADORES).andExpect(status().isForbidden());
        acessar(entregadorAtivo().token(), ENTREGADORES).andExpect(status().isForbidden());
    }
}
