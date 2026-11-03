package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login da credencial APP_ENTREGADOR")
public record LoginAppRequest(
        @Schema(description = "Matrícula do entregador", example = "ENT-000001")
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @Schema(description = "PIN de 6 dígitos", example = "482917")
        @NotBlank(message = "O PIN é obrigatório.") String pin) {
}
