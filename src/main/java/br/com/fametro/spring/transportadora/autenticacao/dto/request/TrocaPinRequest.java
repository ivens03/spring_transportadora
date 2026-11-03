package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Troca do PIN do app (obrigatória quando a chave de 30 dias vence, ou voluntária). */
public record TrocaPinRequest(
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @NotBlank(message = "Informe o PIN atual.") String pinAtual,
        @NotBlank(message = "Informe o novo PIN.") String novoPin) {
}
