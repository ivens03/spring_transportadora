package br.com.fametro.spring.transportadora.usuario.dto.request;

import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial (PATCH): só os campos enviados são alterados; ausentes ficam como estão.
 * {@code status} só aceita BLOQUEADO — para reativar, gera-se um novo código de ativação.
 */
@Schema(description = "Atualização parcial do usuário. Envie só o que muda. status aceita apenas BLOQUEADO.")
public record UsuarioPatchRequest(
        @Schema(description = "Novo nome", example = "Maria Souza Lima")
        @Size(max = 150, message = "O nome tem no máximo 150 caracteres.")
        String nome,
        @Schema(description = "Novo e-mail (login do painel para funcionários)", example = "maria.lima@rotacerta.local")
        @Email(message = "E-mail inválido.") @Size(max = 200, message = "O e-mail tem no máximo 200 caracteres.")
        String email,
        @Schema(description = "Somente BLOQUEADO", example = "BLOQUEADO")
        StatusUsuario status) {
}
