package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Troca da senha do painel (obrigatória quando a chave de 30 dias vence, ou voluntária). */
public record TrocaSenhaRequest(
        @NotBlank(message = "O e-mail é obrigatório.") String email,
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @NotBlank(message = "Informe a nova senha.") String novaSenha) {
}
