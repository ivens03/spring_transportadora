package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Métricas de consumo (só FINANCEIRO). Usa abastecimentos de março/2021, período que nenhum outro teste usa,
 * para os totais da frota serem exatos mesmo com o banco compartilhado entre os testes.
 */
class MetricaConsumoControllerIntegrationTest extends IntegracaoTestBase {

    private static final String MARCO_2021 = "?inicio=2021-03-01&fim=2021-03-31";

    @Test
    void financeiroVeKmPorLitroCustoPorKmEAlertaDeConsumo() throws Exception {
        String financeiro = tokenFuncionario("FINANCEIRO");

        // A: referência 10 km/l, rodou 800 km com 80 l → 10 km/l (dentro do esperado)
        long a = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "GASOLINA", "10"));
        abastecer(financeiro, a, "2021-03-01T08:00:00-03:00", 1000, "40", "240", "GASOLINA").andExpect(status().isCreated());
        abastecer(financeiro, a, "2021-03-10T08:00:00-03:00", 1400, "40", "240", "GASOLINA").andExpect(status().isCreated());
        abastecer(financeiro, a, "2021-03-20T08:00:00-03:00", 1800, "40", "240", "GASOLINA").andExpect(status().isCreated());

        // B: referência 12 km/l, rodou 300 km com 30 l → 10 km/l = 16,67% pior (alerta, tolerância 15%)
        long b = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "FLEX", "12"));
        abastecer(financeiro, b, "2021-03-05T08:00:00-03:00", 5000, "30", "150", "ETANOL").andExpect(status().isCreated());
        abastecer(financeiro, b, "2021-03-25T08:00:00-03:00", 5300, "30", "210", "GASOLINA").andExpect(status().isCreated());

        // fora do período: não conta
        abastecer(financeiro, a, "2021-04-02T08:00:00-03:00", 2200, "40", "240", "GASOLINA").andExpect(status().isCreated());

        acessar(financeiro, VEICULOS + "/" + a + "/metricas-consumo" + MARCO_2021)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeAbastecimentos").value(3))
                .andExpect(jsonPath("$.kmRodados").value(800))
                .andExpect(jsonPath("$.litrosAbastecidos").value(120.0))
                .andExpect(jsonPath("$.gastoTotal").value(720.0))
                .andExpect(jsonPath("$.precoMedioLitro").value(6.0))
                .andExpect(jsonPath("$.consumoMedioKmPorLitro").value(10.0))
                .andExpect(jsonPath("$.custoPorKm").value(0.6))
                .andExpect(jsonPath("$.desvioPercentual").value(0.0))
                .andExpect(jsonPath("$.situacao").value("DENTRO_DO_ESPERADO"));

        acessar(financeiro, VEICULOS + "/" + b + "/metricas-consumo" + MARCO_2021)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumoMedioKmPorLitro").value(10.0))
                .andExpect(jsonPath("$.custoPorKm").value(0.7))  // R$ 210 consumidos / 300 km
                .andExpect(jsonPath("$.desvioPercentual").value(-16.67))
                .andExpect(jsonPath("$.situacao").value("CONSUMO_ACIMA_DO_ESPERADO"));

        acessar(financeiro, METRICAS + MARCO_2021)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2021-03-01"))
                .andExpect(jsonPath("$.fim").value("2021-03-31"))
                .andExpect(jsonPath("$.veiculosAbastecidos").value(2))
                .andExpect(jsonPath("$.veiculosComConsumoAcimaDoEsperado").value(1))
                .andExpect(jsonPath("$.kmRodados").value(1100))
                .andExpect(jsonPath("$.litrosAbastecidos").value(180.0))
                .andExpect(jsonPath("$.gastoTotal").value(1080.0))
                .andExpect(jsonPath("$.consumoMedioKmPorLitro").value(10.0))   // 1100 km / 110 l
                .andExpect(jsonPath("$.custoPorKm").value(0.63))              // (480 + 210) / 1100 = 0,627
                .andExpect(jsonPath("$.veiculos[?(@.veiculoId == " + b + ")].situacao").value("CONSUMO_ACIMA_DO_ESPERADO"))
                .andExpect(jsonPath("$.veiculos[?(@.veiculoId == " + a + ")].kmRodados").value(800));
    }

    @Test
    void veiculoSemDadosOuSemReferencia() throws Exception {
        String financeiro = tokenFuncionario("FINANCEIRO");
        long semReferencia = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "DIESEL", "null"));
        abastecer(financeiro, semReferencia, "2021-02-01T08:00:00-03:00", 100, "50", "300", "DIESEL");
        abastecer(financeiro, semReferencia, "2021-02-11T08:00:00-03:00", 500, "50", "300", "DIESEL");

        acessar(financeiro, VEICULOS + "/" + semReferencia + "/metricas-consumo?inicio=2021-02-01&fim=2021-02-28")
                .andExpect(jsonPath("$.consumoMedioKmPorLitro").value(8.0))
                .andExpect(jsonPath("$.desvioPercentual").doesNotExist())
                .andExpect(jsonPath("$.situacao").value("SEM_REFERENCIA"));

        long novo = cadastrarVeiculo(financeiro, veiculo(placaUnica(), "DIESEL", "8"));
        acessar(financeiro, VEICULOS + "/" + novo + "/metricas-consumo") // padrão: últimos 30 dias
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeAbastecimentos").value(0))
                .andExpect(jsonPath("$.situacao").value("DADOS_INSUFICIENTES"));
    }

    @Test
    void validacoesDoPeriodo() throws Exception {
        String financeiro = tokenFuncionario("FINANCEIRO");
        acessar(financeiro, METRICAS + "?inicio=2021-03-31&fim=2021-03-01").andExpect(status().isUnprocessableContent());
        acessar(financeiro, METRICAS + "?inicio=2020-01-01&fim=2021-12-31").andExpect(status().isUnprocessableContent());
        acessar(financeiro, METRICAS + "?inicio=31/03/2021").andExpect(status().isBadRequest());
        acessar(financeiro, VEICULOS + "/999999/metricas-consumo").andExpect(status().isNotFound());
    }

    @Test
    void soOFinanceiroVeAsMetricas() throws Exception {
        for (String dep : new String[]{"ADMINISTRATIVO", "SUPORTE", "DESENVOLVIMENTO"}) {
            acessar(tokenFuncionario(dep), METRICAS).andExpect(status().isForbidden());
        }
    }
}
