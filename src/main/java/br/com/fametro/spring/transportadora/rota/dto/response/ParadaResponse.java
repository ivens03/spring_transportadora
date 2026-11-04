package br.com.fametro.spring.transportadora.rota.dto.response;

import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import br.com.fametro.spring.transportadora.carga.model.Carga;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Parada da rota (uma carga)")
public record ParadaResponse(int ordem, Long cargaId, String cargaCodigo, String descricao,
                             BigDecimal pesoKg, String destinatarioNome, String destinatarioTelefone,
                             String endereco, String bairro, String cidade, String uf,
                             BigDecimal latitude, BigDecimal longitude,
                             @Schema(description = "Estimativa pelas ruas desde a parada anterior (ou origem)")
                             BigDecimal distanciaDoAnteriorKm,
                             StatusParada status, String motivo, Instant finalizadaEm) {

    public static ParadaResponse de(ParadaRota p) {
        Carga c = p.getCarga();
        return new ParadaResponse(p.getOrdem(), c.getId(), c.getCodigo(), c.getDescricao(), c.getPesoKg(),
                c.getDestinatarioNome(), c.getDestinatarioTelefone(), c.getEndereco(), c.getBairro(), c.getCidade(),
                c.getUf(), c.getLatitude(), c.getLongitude(), p.getDistanciaDoAnteriorKm(), p.getStatus(),
                p.getMotivo(), p.getFinalizadaEm());
    }
}
