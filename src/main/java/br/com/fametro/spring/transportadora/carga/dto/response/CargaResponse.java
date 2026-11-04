package br.com.fametro.spring.transportadora.carga.dto.response;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Carga")
public record CargaResponse(Long id, String codigo, String descricao, BigDecimal pesoKg, BigDecimal volumeM3,
                            String destinatarioNome, String destinatarioTelefone, String endereco, String bairro,
                            String cidade, String uf, String cep, BigDecimal latitude, BigDecimal longitude,
                            StatusCarga status, int tentativasEntrega, String cadastradaPor,
                            Instant criadaEm, Instant atualizadaEm) {

    public static CargaResponse de(Carga c) {
        return new CargaResponse(c.getId(), c.getCodigo(), c.getDescricao(), c.getPesoKg(), c.getVolumeM3(),
                c.getDestinatarioNome(), c.getDestinatarioTelefone(), c.getEndereco(), c.getBairro(), c.getCidade(),
                c.getUf(), c.getCep(), c.getLatitude(), c.getLongitude(), c.getStatus(), c.getTentativasEntrega(),
                c.getCadastradaPor(), c.getCriadaEm(), c.getAtualizadaEm());
    }
}
