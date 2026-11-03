package br.com.fametro.spring.transportadora.veiculo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Consumo da frota inteira no período: totais e a métrica de cada veículo")
public record MetricaFrotaResponse(
        LocalDate inicio, LocalDate fim,
        @Schema(description = "Veículos com pelo menos um abastecimento no período", example = "7")
        int veiculosAbastecidos,
        @Schema(description = "Veículos com km/l pior que a referência além da tolerância", example = "1")
        int veiculosComConsumoAcimaDoEsperado,
        @Schema(example = "8420") long kmRodados,
        @Schema(example = "1083.500") BigDecimal litrosAbastecidos,
        @Schema(description = "R$ gastos com combustível no período", example = "6555.18") BigDecimal gastoTotal,
        @Schema(description = "km/l médio da frota (soma dos km / soma dos litros consumidos)", example = "8.73")
        BigDecimal consumoMedioKmPorLitro,
        @Schema(description = "R$/km médio da frota", example = "0.69") BigDecimal custoPorKm,
        @Schema(description = "Um item por veículo, ordenado pela placa") List<MetricaConsumoResponse> veiculos) {
}
