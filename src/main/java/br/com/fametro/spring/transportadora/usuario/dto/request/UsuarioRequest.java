package br.com.fametro.spring.transportadora.usuario.dto.request;

import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import br.com.fametro.spring.transportadora.usuario.model.enums.Vinculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de usuário (nova matrícula).
 * vinculo EXTERNO -> entregador (departamento deve ficar vazio; e-mail opcional).
 * vinculo INTERNO -> funcionário (departamento e e-mail obrigatórios).
 */
@Schema(description = "Cadastro de usuário. EXTERNO = entregador (sem departamento); INTERNO = funcionário (departamento e e-mail obrigatórios).")
public record UsuarioRequest(
        @Schema(description = "Nome completo", example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório.") @Size(max = 150, message = "O nome tem no máximo 150 caracteres.")
        String nome,
        @Schema(description = "E-mail corporativo (login do painel). Obrigatório para INTERNO.", example = "maria@rotacerta.local")
        @Email(message = "E-mail inválido.") @Size(max = 200, message = "O e-mail tem no máximo 200 caracteres.")
        String email,
        @Schema(description = "EXTERNO (entregador) ou INTERNO (funcionário)", example = "INTERNO")
        @NotNull(message = "Informe o vínculo: EXTERNO (entregador) ou INTERNO (funcionário).")
        Vinculo vinculo,
        @Schema(description = "Somente para INTERNO", example = "FINANCEIRO", nullable = true)
        Departamento departamento) {
}
