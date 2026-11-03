package br.com.fametro.spring.transportadora.veiculo.dto.request;

import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualização parcial (PATCH): só os campos enviados mudam.
 * RENAVAM, ano, tipo e combustível são fixos (o combustível valida o histórico de abastecimentos).
 */
@Schema(description = "Atualização parcial do veículo. Envie só o que muda. RENAVAM, ano, tipo e combustível não mudam.")
public record VeiculoPatchRequest(
        @Schema(description = "Ex.: conversão para placa Mercosul", example = "RIO2A18")
        @Pattern(regexp = VeiculoRequest.REGEX_PLACA, message = "Placa inválida (use ABC1234 ou ABC1D23).")
        String placa,
        @Schema(example = "Fiat") @Size(max = 60, message = "A marca tem no máximo 60 caracteres.")
        String marca,
        @Schema(example = "Fiorino Endurance 1.4") @Size(max = 80, message = "O modelo tem no máximo 80 caracteres.")
        String modelo,
        @Schema(example = "650") @DecimalMin(value = "0.01", message = "A capacidade de carga deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Capacidade de carga inválida.")
        BigDecimal capacidadeCargaKg,
        @Schema(example = "3.10") @DecimalMin(value = "0.01", message = "A capacidade de volume deve ser maior que zero.")
        @Digits(integer = 6, fraction = 2, message = "Capacidade de volume inválida.")
        BigDecimal capacidadeVolumeM3,
        @Schema(example = "11.5") @DecimalMin(value = "0.01", message = "O consumo de referência deve ser maior que zero.")
        @Digits(integer = 4, fraction = 2, message = "Consumo de referência inválido.")
        BigDecimal consumoReferenciaKmPorLitro,
        @Schema(description = "Só pode aumentar (odômetro não volta)", example = "16010")
        @PositiveOrZero(message = "A quilometragem não pode ser negativa.") @Max(value = 9_999_999, message = "Quilometragem inválida.")
        Integer quilometragemAtual,
        @Schema(description = "ATIVO, EM_MANUTENCAO ou INATIVO (inativo não recebe abastecimento)", example = "EM_MANUTENCAO")
        StatusVeiculo status) {

    public boolean vazio() {
        return placa == null && marca == null && modelo == null && capacidadeCargaKg == null && capacidadeVolumeM3 == null
                && consumoReferenciaKmPorLitro == null && quilometragemAtual == null && status == null;
    }
}
