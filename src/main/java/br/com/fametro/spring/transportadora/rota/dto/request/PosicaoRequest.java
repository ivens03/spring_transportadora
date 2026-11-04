package br.com.fametro.spring.transportadora.rota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Uma leitura de GPS do celular")
public record PosicaoRequest(
        @Schema(example = "-3.735100") @NotNull(message = "Informe a latitude.")
        @DecimalMin(value = "-90", message = "Latitude inválida.") @DecimalMax(value = "90", message = "Latitude inválida.")
        @Digits(integer = 2, fraction = 6, message = "Latitude com até 6 casas decimais.")
        BigDecimal latitude,
        @Schema(example = "-38.512300") @NotNull(message = "Informe a longitude.")
        @DecimalMin(value = "-180", message = "Longitude inválida.") @DecimalMax(value = "180", message = "Longitude inválida.")
        @Digits(integer = 3, fraction = 6, message = "Longitude com até 6 casas decimais.")
        BigDecimal longitude,
        @Schema(description = "Hora da leitura no celular", example = "2026-11-04T09:15:30-03:00")
        @NotNull(message = "Informe a hora da leitura.")
        OffsetDateTime registradaEm,
        @Schema(example = "32.5") @DecimalMin(value = "0", message = "Velocidade inválida.")
        @Digits(integer = 4, fraction = 1, message = "Velocidade inválida.")
        BigDecimal velocidadeKmH,
        @Schema(description = "Precisão informada pelo GPS, em metros", example = "8.0")
        @DecimalMin(value = "0", message = "Precisão inválida.") @Digits(integer = 6, fraction = 1, message = "Precisão inválida.")
        BigDecimal precisaoM) {
}
