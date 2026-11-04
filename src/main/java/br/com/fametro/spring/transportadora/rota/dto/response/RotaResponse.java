package br.com.fametro.spring.transportadora.rota.dto.response;

import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = """
        Rota completa: é o que a tela do entregador (e o mapa do Suporte) desenha. trajeto = linha a percorrer
        (origem → paradas → retorno); proximaParada + ultimaPosicao + distância/minutos restantes = navegação.""")
public record RotaResponse(
        Long id, String codigo, StatusRota status,
        Long veiculoId, String veiculoPlaca, String veiculoModelo,
        String entregadorMatricula, String entregadorNome,
        BigDecimal origemLatitude, BigDecimal origemLongitude, boolean retornaOrigem,
        @Schema(description = "Estimativa pelas ruas (linha reta × fator viário), com retorno") BigDecimal distanciaEstimadaKm,
        @Schema(description = "Deslocamento + tempo médio por parada") Integer duracaoEstimadaMin,
        BigDecimal pesoTotalKg, BigDecimal volumeTotalM3,
        List<ParadaResponse> paradas,
        List<PontoResponse> trajeto,
        ProgressoResponse progresso,
        @Schema(description = "Ordem da próxima parada pendente (nulo se não há)") Integer proximaParada,
        @Schema(description = "Última posição de GPS recebida (nulo antes de iniciar)") PosicaoResponse ultimaPosicao,
        @Schema(description = "Da última posição (ou da origem) até a próxima parada, estimado pelas ruas")
        BigDecimal distanciaAteProximaParadaKm,
        @Schema(description = "Minutos estimados até a próxima parada") Integer minutosAteProximaParada,
        String criadaPor, Instant criadaEm, Instant iniciadaEm, Instant concluidaEm, Instant canceladaEm) {
}
