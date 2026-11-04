package br.com.fametro.spring.transportadora.rota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do envio de posições")
public record LotePosicoesResponse(
        @Schema(example = "50") int recebidas,
        @Schema(description = "Novas, gravadas agora", example = "48") int gravadas,
        @Schema(description = "Já existiam (reenvio do app)", example = "2") int duplicadas) {
}
