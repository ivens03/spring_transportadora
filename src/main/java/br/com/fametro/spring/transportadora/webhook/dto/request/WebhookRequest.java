package br.com.fametro.spring.transportadora.webhook.dto.request;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Cadastro de webhook. Sem eventos = recebe todos.")
public record WebhookRequest(
        @Schema(example = "https://minhaloja.com.br/rotacerta/avisos")
        @NotBlank(message = "Informe a URL do webhook.") @Size(max = 500, message = "A URL tem no máximo 500 caracteres.")
        String url,
        @Schema(description = "Eventos que quer receber; vazio/nulo = todos", example = "[\"SAIU_PARA_ENTREGA\",\"LOCALIZACAO\",\"ENTREGUE\"]")
        Set<TipoEventoEncomenda> eventos) {
}
