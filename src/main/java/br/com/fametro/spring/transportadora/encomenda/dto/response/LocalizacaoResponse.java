package br.com.fametro.spring.transportadora.encomenda.dto.response;

import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Última posição de GPS do veículo que leva a encomenda")
public record LocalizacaoResponse(
        @Schema(example = "-3.735100") BigDecimal latitude,
        @Schema(example = "-38.512300") BigDecimal longitude,
        @Schema(example = "32.5") BigDecimal velocidadeKmH,
        @Schema(description = "Hora da leitura no celular do entregador") Instant registradaEm) {

    public static LocalizacaoResponse de(PosicaoRota p) {
        return new LocalizacaoResponse(p.getLatitude(), p.getLongitude(), p.getVelocidadeKmH(), p.getRegistradaEm());
    }
}
