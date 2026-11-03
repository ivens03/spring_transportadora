package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sub-recurso /api/v1/veiculos/{id}/abastecimentos. */
class AbastecimentoControllerIntegrationTest extends IntegracaoTestBase {

    @Test
    void registraAbastecimentoEAtualizaQuilometragem() throws Exception {
        String financeiro = tokenFuncionario("FINANCEIRO");
        long id = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "FLEX", "11"));

        String body = abastecer(financeiro, id, "2026-10-10T08:00:00-03:00", 1000, "40.5", "245.03", "ETANOL")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/veiculos/" + id + "/abastecimentos/\\d+$")))
                .andExpect(jsonPath("$.veiculoId").value(id))
                .andExpect(jsonPath("$.precoLitro").value(6.05))
                .andExpect(jsonPath("$.registradoPor").value(org.hamcrest.Matchers.startsWith("FUN-")))
                .andReturn().getResponse().getContentAsString();
        Number abastecimentoId = JsonPath.read(body, "$.id");

        acessar(financeiro, VEICULOS + "/" + id).andExpect(jsonPath("$.quilometragemAtual").value(1000));
        acessar(financeiro, VEICULOS + "/" + id + "/abastecimentos/" + abastecimentoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.combustivel").value("ETANOL"));
        acessar(financeiro, VEICULOS + "/999999/abastecimentos/" + abastecimentoId).andExpect(status().isNotFound());
        acessar(financeiro, VEICULOS + "/" + id + "/abastecimentos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void odometroPrecisaCaberEntreOsVizinhosPorData() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        long id = cadastrarVeiculo(suporte, veiculo(placaUnica(), "GASOLINA", "10"));
        abastecer(suporte, id, "2026-10-10T08:00:00-03:00", 1000, "40", "240", "GASOLINA").andExpect(status().isCreated());
        abastecer(suporte, id, "2026-10-20T08:00:00-03:00", 1500, "40", "240", "GASOLINA").andExpect(status().isCreated());

        abastecer(suporte, id, "2026-10-15T08:00:00-03:00", 1600, "40", "240", "GASOLINA")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("seguinte")));
        abastecer(suporte, id, "2026-10-15T08:00:00-03:00", 900, "40", "240", "GASOLINA")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("anterior")));
        abastecer(suporte, id, "2026-10-15T08:00:00-03:00", 1200, "40", "240", "GASOLINA")
                .andExpect(status().isCreated()); // lançamento atrasado, mas coerente
        abastecer(suporte, id, "2026-10-15T11:00:00Z", 1250, "40", "240", "GASOLINA")
                .andExpect(status().isConflict()); // mesmo instante (08:00 -03:00 = 11:00Z) = reenvio

        acessar(suporte, VEICULOS + "/" + id).andExpect(jsonPath("$.quilometragemAtual").value(1500)); // não voltou
        acessar(suporte, VEICULOS + "/" + id + "/abastecimentos")
                .andExpect(jsonPath("$.content[0].odometroKm").value(1500)) // mais recente primeiro
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void regrasDoAbastecimento() throws Exception {
        String admin = tokenAdmin();
        long gasolina = cadastrarVeiculo(admin, veiculo(placaUnica(), "GASOLINA", "10"));
        long flex = cadastrarVeiculo(admin, veiculo(placaUnica(), "FLEX", "10"));

        abastecer(admin, gasolina, "2026-10-01T08:00:00-03:00", 200, "30", "180", "DIESEL")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("não pode ser abastecido com DIESEL")));
        abastecer(admin, flex, "2026-10-01T08:00:00-03:00", 200, "30", "180", "FLEX")
                .andExpect(status().isUnprocessableContent()); // informa o que entrou no tanque
        abastecer(admin, flex, "2026-10-01T08:00:00-03:00", 200, "30", "180", "GASOLINA").andExpect(status().isCreated());

        abastecer(admin, gasolina, "2099-01-01T08:00:00-03:00", 200, "30", "180", "GASOLINA")
                .andExpect(status().isBadRequest()); // futuro
        abastecer(admin, gasolina, "2026-10-01T08:00:00-03:00", 200, "0", "180", "GASOLINA")
                .andExpect(status().isBadRequest());
        abastecer(admin, gasolina, "2026-10-01T08:00:00-03:00", 200, "30.1234", "180", "GASOLINA")
                .andExpect(status().isBadRequest()); // 4 casas
        abastecer(admin, 999999, "2026-10-01T08:00:00-03:00", 200, "30", "180", "GASOLINA")
                .andExpect(status().isNotFound());

        enviar(patch(VEICULOS + "/" + gasolina), admin, """
                {"status":"INATIVO"}""").andExpect(status().isOk());
        abastecer(admin, gasolina, "2026-10-01T08:00:00-03:00", 200, "30", "180", "GASOLINA")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("INATIVO")));
    }

    @Test
    void desenvolvimentoEEntregadorNaoRegistram() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin(), veiculo(placaUnica(), "GASOLINA", "10"));
        abastecer(tokenFuncionario("DESENVOLVIMENTO"), id, "2026-10-01T08:00:00-03:00", 200, "30", "180", "GASOLINA")
                .andExpect(status().isForbidden());
    }
}
