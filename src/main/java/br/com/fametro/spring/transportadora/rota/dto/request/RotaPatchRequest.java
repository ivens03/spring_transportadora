package br.com.fametro.spring.transportadora.rota.dto.request;

import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = """
        Mudança de status da rota. Entregador (app): EM_ANDAMENTO = iniciar a rota.
        Suporte (painel): CANCELADA = cancelar antes de sair. CONCLUIDA é automática (última parada baixada).""")
public record RotaPatchRequest(
        @Schema(example = "EM_ANDAMENTO") @NotNull(message = "Informe o status.")
        StatusRota status) {
}
