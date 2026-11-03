package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code novaCredencial} = PIN de 6 dígitos (entregador) ou senha (funcionário). */
public record AtivacaoRequest(
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @NotBlank(message = "O código de ativação é obrigatório.") String codigoAtivacao,
        @NotBlank(message = "Informe o PIN (app) ou a senha (painel).") String novaCredencial) {
}
