package br.com.fametro.spring.transportadora.rota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = """
        Gerar rota: o Suporte escolhe as cargas (ex.: todas AGUARDANDO_ROTA de um bairro), o veículo e o entregador.
        O sistema confere capacidade e disponibilidade e define a ordem das paradas.""")
public record RotaRequest(
        @Schema(example = "1") @NotNull(message = "Informe o veículo.")
        Long veiculoId,
        @Schema(example = "ENT-000001") @NotBlank(message = "Informe a matrícula do entregador.")
        String entregadorMatricula,
        @Schema(description = "Ids das cargas (ordem não importa: o sistema otimiza)", example = "[1, 2, 3]")
        @NotEmpty(message = "Escolha ao menos uma carga.")
        List<@NotNull(message = "Id de carga nulo.") Long> cargaIds,
        @Schema(description = "Ponto de saída. Sem ele, usa o depósito configurado (rotacerta.rota.origem).", example = "-3.731900")
        @DecimalMin(value = "-90", message = "Latitude inválida.") @DecimalMax(value = "90", message = "Latitude inválida.")
        BigDecimal origemLatitude,
        @Schema(example = "-38.526700")
        @DecimalMin(value = "-180", message = "Longitude inválida.") @DecimalMax(value = "180", message = "Longitude inválida.")
        BigDecimal origemLongitude,
        @Schema(description = "Volta ao ponto de saída no fim? Padrão: true", example = "true")
        Boolean retornarAOrigem) {
}
