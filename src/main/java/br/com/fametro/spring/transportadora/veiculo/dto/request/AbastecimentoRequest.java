package br.com.fametro.spring.transportadora.veiculo.dto.request;

import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Abastecimento com tanque cheio (o método de cálculo do km/l depende disso)")
public record AbastecimentoRequest(
        @Schema(description = "Quando abasteceu (ISO-8601 com fuso)", example = "2026-11-03T08:15:00-03:00")
        @NotNull(message = "Informe a data/hora do abastecimento.") @PastOrPresent(message = "O abastecimento não pode estar no futuro.")
        OffsetDateTime dataHora,
        @Schema(description = "Odômetro no momento do abastecimento", example = "15642")
        @NotNull(message = "Informe o odômetro.") @PositiveOrZero(message = "O odômetro não pode ser negativo.")
        @Max(value = 9_999_999, message = "Odômetro inválido.")
        Integer odometroKm,
        @Schema(example = "38.420") @NotNull(message = "Informe os litros.")
        @DecimalMin(value = "0.001", message = "Os litros devem ser maiores que zero.")
        @Digits(integer = 5, fraction = 3, message = "Litros inválidos (até 3 casas decimais).")
        BigDecimal litros,
        @Schema(description = "Valor pago (R$)", example = "232.44") @NotNull(message = "Informe o valor total.")
        @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Valor total inválido (até 2 casas decimais).")
        BigDecimal valorTotal,
        @Schema(description = "O que entrou no tanque: GASOLINA, ETANOL ou DIESEL", example = "GASOLINA")
        @NotNull(message = "Informe o combustível abastecido.")
        Combustivel combustivel,
        @Schema(example = "Posto Aldeota") @Size(max = 120, message = "O posto tem no máximo 120 caracteres.")
        String posto) {
}
