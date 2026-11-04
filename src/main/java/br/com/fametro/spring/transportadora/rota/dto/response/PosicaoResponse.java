package br.com.fametro.spring.transportadora.rota.dto.response;

import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Posição de GPS registrada na rota")
public record PosicaoResponse(BigDecimal latitude, BigDecimal longitude, Instant registradaEm,
                              BigDecimal velocidadeKmH, BigDecimal precisaoM, Instant recebidaEm) {

    public static PosicaoResponse de(PosicaoRota p) {
        return new PosicaoResponse(p.getLatitude(), p.getLongitude(), p.getRegistradaEm(), p.getVelocidadeKmH(),
                p.getPrecisaoM(), p.getRecebidaEm());
    }
}
