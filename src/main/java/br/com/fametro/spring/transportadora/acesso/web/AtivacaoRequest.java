package br.com.fametro.spring.transportadora.acesso.web;

import jakarta.validation.constraints.NotBlank;

/** {@code novaCredencial} = PIN de 6 dígitos (entregador) ou senha (funcionário). */
public record AtivacaoRequest(@NotBlank String matricula, @NotBlank String codigoAtivacao, @NotBlank String novaCredencial) {
}
