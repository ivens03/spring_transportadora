package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Troca do PIN (entregador) ou da senha (funcionário): obrigatória a cada 30 dias, ou voluntária. */
@Schema(description = "Troca de PIN (entregador) ou senha (funcionário). Aceita a credencial atual mesmo vencida.")
public record TrocaCredencialRequest(
        @Schema(description = "PIN ou senha atual", example = "482917")
        @NotBlank(message = "Informe a credencial atual.") String credencialAtual,
        @Schema(description = "Novo PIN (6 dígitos) ou nova senha (≥ 10, letras e números), diferente da atual", example = "905318")
        @NotBlank(message = "Informe a nova credencial.") String novaCredencial) {
}
