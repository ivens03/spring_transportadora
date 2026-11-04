package br.com.fametro.spring.transportadora.webhook.dto.request;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Atualização parcial do webhook: só os campos enviados mudam. eventos vazio = todos.")
public record WebhookPatchRequest(
        @Schema(example = "https://minhaloja.com.br/rotacerta/avisos-v2")
        @Size(max = 500, message = "A URL tem no máximo 500 caracteres.") String url,
        @Schema(example = "[\"ENTREGUE\",\"NAO_ENTREGUE\"]") Set<TipoEventoEncomenda> eventos,
        @Schema(description = "false pausa os avisos (os novos eventos não entram na fila)", example = "false") Boolean ativo) {

    public boolean vazio() {
        return url == null && eventos == null && ativo == null;
    }
}
