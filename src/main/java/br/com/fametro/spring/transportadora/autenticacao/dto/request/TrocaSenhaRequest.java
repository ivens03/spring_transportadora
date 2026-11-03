package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Troca da senha do painel (obrigatória quando vence em 30 dias, ou voluntária)")
public record TrocaSenhaRequest(
        @Schema(description = "E-mail corporativo", example = "maria@rotacerta.local")
        @NotBlank(message = "O e-mail é obrigatório.") String email,
        @Schema(description = "Senha atual (mesmo vencida)", example = "SenhaForte2026")
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @Schema(description = "Nova senha: ≥ 10 caracteres, letras e números, diferente da atual", example = "NovaSenha2026x")
        @NotBlank(message = "Informe a nova senha.") String novaSenha) {
}
