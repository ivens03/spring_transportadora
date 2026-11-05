package br.com.fametro.spring.transportadora.webhook.dto.response;

import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResumoResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.RastreioResponse;
import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Corpo JSON que o servidor do cliente recebe (POST). Mesmo formato do GET /encomendas/{codigo}, sem a linha do
 * tempo. Os avisos podem chegar fora de ordem ou repetidos (retentativa): ordene por {@code ocorridoEm} e
 * ignore o que já processou pelo header X-RotaCerta-Entrega.
 */
@Schema(description = "Corpo do aviso enviado ao webhook do cliente")
public record WebhookPayload(
        @Schema(example = "LOCALIZACAO") TipoEventoEncomenda evento,
        @Schema(example = "Localização do veículo atualizada") String descricao,
        Instant ocorridoEm,
        @Schema(description = "Nulo no PING") EncomendaResumoResponse encomenda,
        @Schema(description = "Rota, posição do veículo e previsão (nulo fora de rota)") RastreioResponse rastreio,
        @Schema(description = "Motivo da não entrega; na LOCALIZACAO, a frase com a rua e quanto falta",
                example = "Na Avenida Bezerra de Menezes, São Gerardo · faltam ~12 min (3,4 km) · é a próxima entrega")
        String detalhe) {
}
