package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Troca do PIN do app (obrigatória quando vence em 30 dias, ou voluntária)")
public record TrocaPinRequest(
        @Schema(description = "Matrícula do entregador", example = "ENT-000001")
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @Schema(description = "PIN atual (mesmo vencido)", example = "482917")
        @NotBlank(message = "Informe o PIN atual.") String pinAtual,
        @Schema(description = "Novo PIN de 6 dígitos, diferente do atual", example = "905318")
        @NotBlank(message = "Informe o novo PIN.") String novoPin) {
}
