package br.com.fametro.spring.transportadora.rota.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Andamento das paradas")
public record ProgressoResponse(int totalParadas, int entregues, int naoEntregues, int pendentes,
                                @Schema(description = "Paradas baixadas / total × 100", example = "40") int percentual) {
}
