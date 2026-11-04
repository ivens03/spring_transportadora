package br.com.fametro.spring.transportadora.rota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ponto do trajeto planejado, na ordem em que deve ser percorrido")
public record PontoResponse(
        @Schema(description = "ORIGEM, PARADA ou RETORNO") String tipo,
        @Schema(description = "Ordem da parada (nulo na origem/retorno)") Integer ordem,
        BigDecimal latitude, BigDecimal longitude) {
}
