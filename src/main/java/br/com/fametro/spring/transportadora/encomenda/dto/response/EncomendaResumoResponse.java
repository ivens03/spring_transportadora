package br.com.fametro.spring.transportadora.encomenda.dto.response;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/** A encomenda como o cliente vê (sem dados internos: peso de rota, quem cadastrou etc.). */
@Schema(description = "Encomenda do cliente")
public record EncomendaResumoResponse(
        @Schema(example = "CRG-000005") String codigo,
        @Schema(example = "Caixa de eletrônicos") String descricao,
        @Schema(example = "EM_TRANSITO") StatusCarga status,
        @Schema(example = "Ana Ribeiro") String destinatarioNome,
        @Schema(example = "Av. Santos Dumont, 1500") String endereco,
        @Schema(example = "Aldeota") String bairro,
        @Schema(example = "Fortaleza") String cidade,
        @Schema(example = "CE") String uf,
        @Schema(example = "60150160") String cep,
        @Schema(example = "-3.737400") BigDecimal latitude,
        @Schema(example = "-38.497700") BigDecimal longitude,
        @Schema(description = "Tentativas de entrega que não deram certo", example = "0") int tentativasEntrega,
        Instant criadaEm,
        Instant atualizadaEm) {

    public static EncomendaResumoResponse de(Carga c) {
        return new EncomendaResumoResponse(c.getCodigo(), c.getDescricao(), c.getStatus(), c.getDestinatarioNome(),
                c.getEndereco(), c.getBairro(), c.getCidade(), c.getUf(), c.getCep(), c.getLatitude(), c.getLongitude(),
                c.getTentativasEntrega(), c.getCriadaEm(), c.getAtualizadaEm());
    }
}
