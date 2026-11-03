package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Login da credencial APP_ENTREGADOR. */
public record LoginAppRequest(
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @NotBlank(message = "O PIN é obrigatório.") String pin) {
}
