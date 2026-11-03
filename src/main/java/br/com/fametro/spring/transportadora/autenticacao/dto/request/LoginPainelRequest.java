package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login da credencial PAINEL_WEB")
public record LoginPainelRequest(
        @Schema(description = "E-mail corporativo", example = "admin@rotacerta.local")
        @NotBlank(message = "O e-mail é obrigatório.") String email,
        @Schema(description = "Senha do painel", example = "SenhaForte2026")
        @NotBlank(message = "A senha é obrigatória.") String senha) {
}
