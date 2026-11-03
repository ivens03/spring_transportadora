package br.com.fametro.spring.transportadora.veiculo.dto.request;

import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Cadastro de veículo da frota")
public record VeiculoRequest(
        @Schema(description = "Placa antiga (ABC1234) ou Mercosul (ABC1D23); hífen e minúsculas são aceitos", example = "RIO2A18")
        @NotBlank(message = "Informe a placa.")
        @Pattern(regexp = VeiculoRequest.REGEX_PLACA, message = "Placa inválida (use ABC1234 ou ABC1D23).")
        String placa,
        @Schema(description = "RENAVAM com 11 dígitos (opcional)", example = "01234567890")
        @Pattern(regexp = "^[0-9]{11}$", message = "O RENAVAM tem 11 dígitos.")
        String renavam,
        @Schema(example = "Fiat") @NotBlank(message = "Informe a marca.") @Size(max = 60, message = "A marca tem no máximo 60 caracteres.")
        String marca,
        @Schema(example = "Fiorino Endurance 1.4") @NotBlank(message = "Informe o modelo.") @Size(max = 80, message = "O modelo tem no máximo 80 caracteres.")
        String modelo,
        @Schema(example = "2023") @NotNull(message = "Informe o ano de fabricação.") @Min(value = 1980, message = "Ano de fabricação inválido.")
        Integer anoFabricacao,
        @Schema(example = "UTILITARIO") @NotNull(message = "Informe o tipo do veículo.")
        TipoVeiculo tipo,
        @Schema(description = "FLEX aceita abastecimento com GASOLINA ou ETANOL", example = "FLEX")
        @NotNull(message = "Informe o combustível.")
        Combustivel combustivel,
        @Schema(description = "Carga útil em kg", example = "650")
        @NotNull(message = "Informe a capacidade de carga.") @DecimalMin(value = "0.01", message = "A capacidade de carga deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Capacidade de carga inválida.")
        BigDecimal capacidadeCargaKg,
        @Schema(description = "Volume do baú/caçamba em m³", example = "3.10")
        @NotNull(message = "Informe a capacidade de volume.") @DecimalMin(value = "0.01", message = "A capacidade de volume deve ser maior que zero.")
        @Digits(integer = 6, fraction = 2, message = "Capacidade de volume inválida.")
        BigDecimal capacidadeVolumeM3,
        @Schema(description = "km/l esperado (fabricante ou histórico). Sem ele as métricas não geram alerta.", example = "11.5")
        @DecimalMin(value = "0.01", message = "O consumo de referência deve ser maior que zero.")
        @Digits(integer = 4, fraction = 2, message = "Consumo de referência inválido.")
        BigDecimal consumoReferenciaKmPorLitro,
        @Schema(description = "Odômetro no cadastro (padrão 0)", example = "15230")
        @PositiveOrZero(message = "A quilometragem não pode ser negativa.") @Max(value = 9_999_999, message = "Quilometragem inválida.")
        Integer quilometragemAtual) {

    /** Antiga ou Mercosul, com ou sem hífen, qualquer caixa. */
    public static final String REGEX_PLACA = "(?i)^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$";
}
