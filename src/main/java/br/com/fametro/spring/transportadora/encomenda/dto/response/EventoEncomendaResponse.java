package br.com.fametro.spring.transportadora.encomenda.dto.response;

import br.com.fametro.spring.transportadora.encomenda.model.EventoEncomenda;
import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Marco da linha do tempo da encomenda")
public record EventoEncomendaResponse(
        @Schema(example = "SAIU_PARA_ENTREGA") TipoEventoEncomenda tipo,
        @Schema(example = "Saiu para entrega") String descricao,
        @Schema(example = "ROT-000003") String rotaCodigo,
        @Schema(description = "Ex.: motivo da não entrega", example = "Destinatário ausente") String detalhe,
        Instant ocorridoEm) {

    public static EventoEncomendaResponse de(EventoEncomenda e) {
        return new EventoEncomendaResponse(e.getTipo(), e.getTipo().getDescricao(), e.getRotaCodigo(), e.getDetalhe(),
                e.getOcorridoEm());
    }
}
