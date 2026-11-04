package br.com.fametro.spring.transportadora.carga.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recurso /api/v1/cargas (só SUPORTE). */
class CargaControllerIntegrationTest extends IntegracaoTestBase {

    @Test
    void suporteCadastraCarga() throws Exception {
        enviar(post(CARGAS), tokenFuncionario("SUPORTE"), carga("Aldeota", "-3.7374", "-38.4977", "12.5"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/cargas/\\d+$")))
                .andExpect(jsonPath("$.codigo").value(matchesPattern("CRG-\\d{6}")))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ROTA"))
                .andExpect(jsonPath("$.uf").value("CE"))
                .andExpect(jsonPath("$.tentativasEntrega").value(0));
    }

    @Test
    void validacoesDoCadastro() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        enviar(post(CARGAS), suporte, carga("Aldeota", "91", "-38.4977", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'latitude')].mensagem").value("Latitude inválida."));
        enviar(post(CARGAS), suporte, carga("Aldeota", "-3.7374", "-38.4977", "0")).andExpect(status().isBadRequest());
        enviar(post(CARGAS), suporte, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void filtroPorStatusCidadeEBairroParaEscolherAsCargasDeUmDestino() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        String bairro = "Bairro-" + UUID.randomUUID().toString().substring(0, 8);
        long a = cadastrarCarga(suporte, carga(bairro, "-3.74", "-38.50", "1"));
        cadastrarCarga(suporte, carga(bairro, "-3.75", "-38.51", "1"));
        cadastrarCarga(suporte, carga("Outro", "-3.75", "-38.51", "1"));

        acessar(suporte, CARGAS + "?status=AGUARDANDO_ROTA&cidade=FORTALEZA&bairro=" + bairro.toLowerCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(a)); // mais antiga primeiro
        acessar(suporte, CARGAS + "?status=VOANDO").andExpect(status().isBadRequest());
        acessar(suporte, CARGAS + "/999999").andExpect(status().isNotFound());
    }

    @Test
    void patchSoEnquantoAguardandoRota() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        long id = cadastrarCarga(suporte, carga("Meireles", "-3.72", "-38.49", "5"));

        enviar(patch(CARGAS + "/" + id), suporte, """
                {"pesoKg":7.5,"destinatarioNome":"Novo Nome"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoKg").value(7.5))
                .andExpect(jsonPath("$.destinatarioNome").value("Novo Nome"))
                .andExpect(jsonPath("$.bairro").value("Meireles"));
        enviar(patch(CARGAS + "/" + id), suporte, """
                {"latitude":-3.70}""")
                .andExpect(status().isUnprocessableContent()); // destino muda em bloco
        enviar(patch(CARGAS + "/" + id), suporte, "{}").andExpect(status().isUnprocessableContent());
        enviar(patch(CARGAS + "/" + id), suporte, """
                {"status":"ENTREGUE"}""").andExpect(status().isUnprocessableContent());
        enviar(patch(CARGAS + "/" + id), suporte, """
                {"status":"CANCELADA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        enviar(patch(CARGAS + "/" + id), suporte, """
                {"pesoKg":1}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void soOSuporteMexeEmCargas() throws Exception {
        for (String dep : new String[]{"ADMINISTRATIVO", "FINANCEIRO", "DESENVOLVIMENTO"}) {
            enviar(post(CARGAS), tokenFuncionario(dep), carga("Centro", "-3.72", "-38.52", "1"))
                    .andExpect(status().isForbidden());
        }
    }
}
