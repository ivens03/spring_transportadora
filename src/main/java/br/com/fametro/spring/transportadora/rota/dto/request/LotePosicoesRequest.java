package br.com.fametro.spring.transportadora.rota.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = """
        Lote de posições (offline-first): o app guarda as leituras sem sinal e envia tudo quando reconecta.
        Reenviar o mesmo lote não duplica nada.""")
public record LotePosicoesRequest(
        @NotEmpty(message = "Envie ao menos uma posição.")
        List<@Valid @NotNull(message = "Posição nula.") PosicaoRequest> posicoes) {
}
