package br.com.fametro.spring.transportadora.rota.dto.request;

import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "Baixa da parada pelo entregador. Repetir o mesmo envio é inofensivo (reenvio do app).")
public record ParadaPatchRequest(
        @Schema(description = "ENTREGUE ou NAO_ENTREGUE", example = "ENTREGUE") @NotNull(message = "Informe o status.")
        StatusParada status,
        @Schema(description = "Obrigatório quando NAO_ENTREGUE", example = "Destinatário ausente")
        @Size(max = 300, message = "O motivo tem no máximo 300 caracteres.")
        String motivo,
        @Schema(description = "Hora real da baixa no celular (app offline). Padrão: agora", example = "2026-11-04T10:42:00-03:00")
        OffsetDateTime finalizadaEm) {
}
