package br.com.fametro.spring.transportadora.encomenda.dto.response;

import br.com.fametro.spring.transportadora.encomenda.service.LocalizadorRua.Rua;
import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Schema(description = "Última posição de GPS do veículo que leva a encomenda")
public record LocalizacaoResponse(
        @Schema(example = "-3.735100") BigDecimal latitude,
        @Schema(example = "-38.512300") BigDecimal longitude,
        @Schema(example = "32.5") BigDecimal velocidadeKmH,
        @Schema(description = "Hora da leitura no celular do entregador") Instant registradaEm,
        @Schema(description = "Rua onde o veículo está (OpenStreetMap); nulo se o mapa não souber",
                example = "Avenida Bezerra de Menezes") String rua,
        @Schema(description = "Bairro dessa rua; nulo se o mapa não souber", example = "São Gerardo") String bairro) {

    public static LocalizacaoResponse de(PosicaoRota p, Optional<Rua> rua) {
        return new LocalizacaoResponse(p.getLatitude(), p.getLongitude(), p.getVelocidadeKmH(), p.getRegistradaEm(),
                rua.map(Rua::logradouro).orElse(null), rua.map(Rua::bairro).orElse(null));
    }
}
