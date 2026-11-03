package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Login da credencial PAINEL_WEB. */
public record LoginPainelRequest(
        @NotBlank(message = "O e-mail é obrigatório.") String email,
        @NotBlank(message = "A senha é obrigatória.") String senha) {
}
