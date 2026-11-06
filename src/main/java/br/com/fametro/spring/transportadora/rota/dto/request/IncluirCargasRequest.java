package br.com.fametro.spring.transportadora.rota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = """
        Incluir cargas numa rota que ainda não saiu (PLANEJADA): o entregador já escolhido leva mais estas entregas.
        A ordem de todas as paradas é recalculada.""")
public record IncluirCargasRequest(
        @Schema(description = "Ids das cargas AGUARDANDO_ROTA", example = "[5]")
        @NotEmpty(message = "Escolha ao menos uma carga.")
        List<@NotNull(message = "Id de carga nulo.") Long> cargaIds) {
}
