package br.com.fametro.spring.transportadora.veiculo.dto.response;

import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Veículo da frota")
public record VeiculoResponse(Long id, String placa, String renavam, String marca, String modelo, Integer anoFabricacao,
                              TipoVeiculo tipo, Combustivel combustivel, BigDecimal capacidadeCargaKg,
                              BigDecimal capacidadeVolumeM3, BigDecimal consumoReferenciaKmPorLitro,
                              Integer quilometragemAtual, StatusVeiculo status, String cadastradoPor,
                              Instant criadoEm, Instant atualizadoEm) {

    public static VeiculoResponse de(Veiculo v) {
        return new VeiculoResponse(v.getId(), v.getPlaca(), v.getRenavam(), v.getMarca(), v.getModelo(),
                v.getAnoFabricacao(), v.getTipo(), v.getCombustivel(), v.getCapacidadeCargaKg(),
                v.getCapacidadeVolumeM3(), v.getConsumoReferenciaKmPorLitro(), v.getQuilometragemAtual(),
                v.getStatus(), v.getCadastradoPor(), v.getCriadoEm(), v.getAtualizadoEm());
    }
}
