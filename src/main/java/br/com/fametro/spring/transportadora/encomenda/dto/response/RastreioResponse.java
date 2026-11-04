package br.com.fametro.spring.transportadora.encomenda.dto.response;

import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Onde está a encomenda. Só existe enquanto ela está numa rota (ROTEIRIZADA, EM_TRANSITO) ou acabou de ser
 * entregue. Localização e previsão só com a rota EM_ANDAMENTO e a encomenda ainda a caminho.
 */
@Schema(description = "Rota, posição do veículo e previsão de chegada da encomenda")
public record RastreioResponse(
        @Schema(example = "ROT-000003") String rotaCodigo,
        @Schema(example = "EM_ANDAMENTO") StatusRota rotaStatus,
        @Schema(description = "Primeiro nome do entregador", example = "João") String entregador,
        @Schema(example = "Fiorino") String veiculoModelo,
        @Schema(example = "ABC1D23") String veiculoPlaca,
        @Schema(description = "Ordem da encomenda na rota", example = "3") int ordemParada,
        @Schema(description = "Entregas pendentes antes desta (0 = é a próxima)", example = "2") Integer entregasAntes,
        @Schema(description = "Nulo antes de a rota sair ou depois da entrega") LocalizacaoResponse localizacao,
        @Schema(description = "Distância estimada pelas ruas até o destino, passando pelas entregas anteriores", example = "4.80")
        BigDecimal distanciaKm,
        @Schema(description = "Minutos estimados até a chegada (deslocamento + tempo das entregas anteriores)", example = "22")
        Integer previsaoMinutos,
        @Schema(description = "Hora estimada de chegada") Instant previsaoChegadaEm,
        @Schema(description = "Quando foi entregue (se foi)") Instant entregueEm) {
}
