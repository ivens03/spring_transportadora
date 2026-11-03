package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.veiculo.dto.response.MetricaConsumoResponse;
import br.com.fametro.spring.transportadora.veiculo.dto.response.MetricaFrotaResponse;
import br.com.fametro.spring.transportadora.veiculo.service.MetricaConsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Métricas de consumo — somente leitura, permissão consumo:ver-metricas (FINANCEIRO):
 * <pre>
 * GET /api/v1/metricas-consumo?inicio=&amp;fim=                  frota inteira (totais + um item por veículo)
 * GET /api/v1/veiculos/{veiculoId}/metricas-consumo?inicio=&amp;fim=  um veículo
 * </pre>
 * Período: datas inclusivas no fuso da frota; sem datas = últimos 30 dias.
 */
@Tag(name = "Métricas de consumo", description = "km/l, R$/km, gasto e alerta de consumo da frota (permissão consumo:ver-metricas — FINANCEIRO)")
@RestController
@RequiredArgsConstructor
public class MetricaConsumoController {

    private final MetricaConsumoService metricaConsumoService;

    @Operation(summary = "Métricas de consumo da frota",
            description = """
                    Totais da frota e a métrica de cada veículo no período. Método do tanque cheio: o 1º abastecimento
                    do período é o ponto de partida. situacao CONSUMO_ACIMA_DO_ESPERADO = km/l mais de 15% abaixo da referência.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas calculadas"),
            @ApiResponse(responseCode = "400", description = "Data em formato inválido (use AAAA-MM-DD)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "inicio depois de fim ou período maior que 366 dias", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/api/v1/metricas-consumo")
    public ResponseEntity<MetricaFrotaResponse> daFrota(
            @Parameter(description = "Data inicial (inclusiva). Padrão: fim − 29 dias", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Data final (inclusiva). Padrão: hoje", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(metricaConsumoService.daFrota(inicio, fim));
    }

    @Operation(summary = "Métricas de consumo de um veículo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas calculadas"),
            @ApiResponse(responseCode = "400", description = "Data em formato inválido (use AAAA-MM-DD)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "inicio depois de fim ou período maior que 366 dias", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/api/v1/veiculos/{veiculoId}/metricas-consumo")
    public ResponseEntity<MetricaConsumoResponse> doVeiculo(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long veiculoId,
            @Parameter(description = "Data inicial (inclusiva). Padrão: fim − 29 dias", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Data final (inclusiva). Padrão: hoje", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(metricaConsumoService.doVeiculo(veiculoId, inicio, fim));
    }
}
