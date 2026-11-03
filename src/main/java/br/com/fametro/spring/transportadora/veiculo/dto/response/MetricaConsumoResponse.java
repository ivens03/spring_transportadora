package br.com.fametro.spring.transportadora.veiculo.dto.response;

import br.com.fametro.spring.transportadora.veiculo.model.enums.SituacaoConsumo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = """
        Métricas de consumo de um veículo no período (método do tanque cheio: o 1º abastecimento do período
        é o ponto de partida; os litros dele não entram no km/l, só no gasto total).""")
public record MetricaConsumoResponse(
        Long veiculoId, String placa, String modelo, TipoVeiculo tipo,
        LocalDate inicio, LocalDate fim,
        @Schema(example = "4") int quantidadeAbastecimentos,
        @Schema(description = "Último odômetro − primeiro odômetro do período", example = "1250")
        int kmRodados,
        @Schema(description = "Todos os litros abastecidos no período", example = "152.300")
        BigDecimal litrosAbastecidos,
        @Schema(description = "Tudo o que foi pago no período (R$)", example = "921.40")
        BigDecimal gastoTotal,
        @Schema(description = "gastoTotal / litrosAbastecidos (R$/l)", example = "6.050")
        BigDecimal precoMedioLitro,
        @Schema(description = "km rodados / litros consumidos (sem o 1º abastecimento)", example = "10.95")
        BigDecimal consumoMedioKmPorLitro,
        @Schema(description = "Custo do combustível consumido por km (R$/km)", example = "0.55")
        BigDecimal custoPorKm,
        @Schema(example = "11.50") BigDecimal consumoReferenciaKmPorLitro,
        @Schema(description = "(consumo médio − referência) / referência × 100. Negativo = rende menos que o esperado.", example = "-4.78")
        BigDecimal desvioPercentual,
        SituacaoConsumo situacao) {
}
