package br.com.fametro.spring.transportadora.encomenda.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Encomenda com rastreio (rota, posição do veículo, previsão) e linha do tempo")
public record EncomendaResponse(
        EncomendaResumoResponse encomenda,
        @Schema(description = "Nulo enquanto a encomenda não está numa rota") RastreioResponse rastreio,
        List<EventoEncomendaResponse> eventos) {
}
