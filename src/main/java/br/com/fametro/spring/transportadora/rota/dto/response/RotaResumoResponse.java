package br.com.fametro.spring.transportadora.rota.dto.response;

import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Rota na listagem")
public record RotaResumoResponse(Long id, String codigo, StatusRota status, Long veiculoId, String veiculoPlaca,
                                 String entregadorMatricula, String entregadorNome, int totalParadas,
                                 int paradasBaixadas, BigDecimal distanciaEstimadaKm, Integer duracaoEstimadaMin,
                                 Instant criadaEm, Instant iniciadaEm, Instant concluidaEm) {

    public static RotaResumoResponse de(Rota r) {
        int baixadas = (int) r.getParadas().stream().filter(p -> !p.pendente()).count();
        return new RotaResumoResponse(r.getId(), r.getCodigo(), r.getStatus(), r.getVeiculo().getId(),
                r.getVeiculo().getPlaca(), r.getEntregador().getMatricula(), r.getEntregador().getNome(),
                r.getParadas().size(), baixadas, r.getDistanciaEstimadaKm(), r.getDuracaoEstimadaMin(),
                r.getCriadaEm(), r.getIniciadaEm(), r.getConcluidaEm());
    }
}
