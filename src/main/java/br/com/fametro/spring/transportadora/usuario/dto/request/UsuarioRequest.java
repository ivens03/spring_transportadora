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
 * vinculo CLIENTE  -> cliente do Portal do Cliente (sem departamento; e-mail obrigatório).
 */
@Schema(description = "Cadastro de usuário. EXTERNO = entregador (sem departamento); INTERNO = funcionário (departamento e e-mail obrigatórios); CLIENTE = cliente do portal (e-mail obrigatório, sem departamento).")
public record UsuarioRequest(
        @Schema(description = "Nome completo", example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório.") @Size(max = 150, message = "O nome tem no máximo 150 caracteres.")
        String nome,
        @Schema(description = "E-mail (login do painel/portal). Obrigatório para INTERNO e CLIENTE.", example = "maria@rotacerta.local")
        @Email(message = "E-mail inválido.") @Size(max = 200, message = "O e-mail tem no máximo 200 caracteres.")
        String email,
        @Schema(description = "EXTERNO (entregador), INTERNO (funcionário) ou CLIENTE (portal do cliente)", example = "INTERNO")
        @NotNull(message = "Informe o vínculo: EXTERNO (entregador), INTERNO (funcionário) ou CLIENTE.")
        Vinculo vinculo,
        @Schema(description = "Somente para INTERNO", example = "FINANCEIRO", nullable = true)
        Departamento departamento) {
}
