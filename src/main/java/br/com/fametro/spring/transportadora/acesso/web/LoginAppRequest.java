package br.com.fametro.spring.transportadora.acesso.web;

import jakarta.validation.constraints.NotBlank;

public record LoginAppRequest(@NotBlank String matricula, @NotBlank String pin) {
}
