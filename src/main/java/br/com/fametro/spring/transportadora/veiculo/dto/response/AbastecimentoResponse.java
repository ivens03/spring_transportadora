package br.com.fametro.spring.transportadora.veiculo.dto.response;

import br.com.fametro.spring.transportadora.veiculo.model.Abastecimento;
import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Schema(description = "Abastecimento registrado")
public record AbastecimentoResponse(Long id, Long veiculoId, Instant dataHora, Integer odometroKm, BigDecimal litros,
                                    BigDecimal valorTotal,
                                    @Schema(description = "valorTotal / litros (R$/l)", example = "6.050")
                                    BigDecimal precoLitro,
                                    Combustivel combustivel, String posto, String registradoPor, Instant criadoEm) {

    public static AbastecimentoResponse de(Abastecimento a) {
        return new AbastecimentoResponse(a.getId(), a.getVeiculo().getId(), a.getDataHora(), a.getOdometroKm(),
                a.getLitros(), a.getValorTotal(), a.getValorTotal().divide(a.getLitros(), 3, RoundingMode.HALF_UP),
                a.getCombustivel(), a.getPosto(), a.getRegistradoPor(), a.getCriadoEm());
    }
}
