package br.com.fametro.spring.transportadora.acesso.web;

import jakarta.validation.constraints.NotBlank;

public record LoginPainelRequest(@NotBlank String email, @NotBlank String senha) {
}
