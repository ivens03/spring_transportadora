package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recurso /api/v1/veiculos (ADMINISTRATIVO, SUPORTE e FINANCEIRO cadastram a frota). */
class VeiculoControllerIntegrationTest extends IntegracaoTestBase {

    @Test
    void suporteCadastraVeiculoComPlacaNormalizada() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        String placa = placaUnica();
        String placaDigitada = (placa.substring(0, 3) + "-" + placa.substring(3)).toLowerCase();

        enviar(post(VEICULOS), suporte, veiculo(placaDigitada, "FLEX", "11.5"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/veiculos/\\d+$")))
                .andExpect(jsonPath("$.placa").value(placa))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.combustivel").value("FLEX"))
                .andExpect(jsonPath("$.quilometragemAtual").value(100))
                .andExpect(jsonPath("$.cadastradoPor").value(org.hamcrest.Matchers.startsWith("FUN-")));
    }

    @Test
    void administrativoEFinanceiroTambemCadastramEConsultam() throws Exception {
        for (String dep : new String[]{"ADMINISTRATIVO", "FINANCEIRO"}) {
            String token = tokenFuncionario(dep);
            long id = cadastrarVeiculo(token, veiculo(placaUnica(), "DIESEL", "null"));
            acessar(token, VEICULOS + "/" + id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.consumoReferenciaKmPorLitro").doesNotExist());
        }
    }

    @Test
    void validacoesDoCadastro() throws Exception {
        String admin = tokenAdmin();
        String placa = placaUnica();
        cadastrarVeiculo(admin, veiculo(placa, "GASOLINA", "10"));

        enviar(post(VEICULOS), admin, veiculo(placa, "GASOLINA", "10"))
                .andExpect(status().isConflict());                                   // placa repetida
        enviar(post(VEICULOS), admin, veiculo("AB12345", "GASOLINA", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'placa')].mensagem").value("Placa inválida (use ABC1234 ou ABC1D23)."));
        enviar(post(VEICULOS), admin, veiculo(placaUnica(), "GASOLINA", "10").replace("2023", "2099"))
                .andExpect(status().isUnprocessableContent());                       // ano no futuro
        enviar(post(VEICULOS), admin, veiculo(placaUnica(), "GASOLINA", "10").replace("\"capacidadeCargaKg\":650", "\"capacidadeCargaKg\":0"))
                .andExpect(status().isBadRequest());
        enviar(post(VEICULOS), admin, """
                {"placa":"%s"}""".formatted(placaUnica()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(7));
        enviar(post(VEICULOS), admin, veiculo(placaUnica(), "ALCOOL", "10"))
                .andExpect(status().isBadRequest());                                 // enum inexistente

        String renavam = String.valueOf(10_000_000_000L + (long) (Math.random() * 89_999_999_999L));
        String comRenavam = veiculo(placaUnica(), "DIESEL", "8").replace("{", "{\"renavam\":\"" + renavam + "\",");
        cadastrarVeiculo(admin, comRenavam);
        enviar(post(VEICULOS), admin, veiculo(placaUnica(), "DIESEL", "8").replace("{", "{\"renavam\":\"" + renavam + "\","))
                .andExpect(status().isConflict());                                   // RENAVAM repetido
    }

    @Test
    void listagemPaginadaComFiltros() throws Exception {
        String admin = tokenAdmin();
        long id = cadastrarVeiculo(admin, veiculo(placaUnica(), "DIESEL", "7").replace("UTILITARIO", "VUC"));

        acessar(admin, VEICULOS + "?tipo=VUC&size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].tipo").value("VUC"))
                .andExpect(jsonPath("$.content[?(@.tipo != 'VUC')]").isEmpty())
                .andExpect(jsonPath("$.page.size").value(100));
        acessar(admin, VEICULOS + "?status=INATIVO&tipo=VUC")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")]").isEmpty());
        acessar(admin, VEICULOS + "?tipo=FOGUETE").andExpect(status().isBadRequest());
        acessar(admin, VEICULOS + "?sort=naoExiste").andExpect(status().isBadRequest());
        acessar(admin, VEICULOS + "/999999").andExpect(status().isNotFound());
    }

    @Test
    void patchAlteraSoOQueFoiEnviado() throws Exception {
        String financeiro = tokenFuncionario("FINANCEIRO");
        long id = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "GASOLINA", "12"));
        String outraPlaca = placaUnica();
        cadastrarVeiculo(financeiro, veiculo(outraPlaca, "GASOLINA", "12"));

        enviar(patch(VEICULOS + "/" + id), financeiro, """
                {"status":"EM_MANUTENCAO","consumoReferenciaKmPorLitro":11.2,"quilometragemAtual":500}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_MANUTENCAO"))
                .andExpect(jsonPath("$.consumoReferenciaKmPorLitro").value(11.2))
                .andExpect(jsonPath("$.quilometragemAtual").value(500))
                .andExpect(jsonPath("$.marca").value("Fiat"));

        enviar(patch(VEICULOS + "/" + id), financeiro, "{}").andExpect(status().isUnprocessableContent());
        enviar(patch(VEICULOS + "/" + id), financeiro, """
                {"quilometragemAtual":499}""").andExpect(status().isUnprocessableContent()); // km não volta
        enviar(patch(VEICULOS + "/" + id), financeiro, """
                {"marca":"  "}""").andExpect(status().isUnprocessableContent());
        enviar(patch(VEICULOS + "/" + id), financeiro, """
                {"placa":"%s"}""".formatted(outraPlaca)).andExpect(status().isConflict());
        enviar(patch(VEICULOS + "/999999"), financeiro, """
                {"marca":"VW"}""").andExpect(status().isNotFound());
    }
}
