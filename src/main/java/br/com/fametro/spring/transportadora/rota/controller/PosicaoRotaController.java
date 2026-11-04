package br.com.fametro.spring.transportadora.rota.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.rota.dto.request.LotePosicoesRequest;
import br.com.fametro.spring.transportadora.rota.dto.response.LotePosicoesResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.PosicaoResponse;
import br.com.fametro.spring.transportadora.rota.service.RastreamentoService;
import br.com.fametro.spring.transportadora.rota.service.Solicitante;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Sub-recurso posições (rastreamento GPS da rota):
 * <pre>
 * POST /api/v1/rotas/{id}/posicoes            app envia lote de leituras (offline-first)        201
 * GET  /api/v1/rotas/{id}/posicoes?desde=     rastro percorrido (painel: mapa; app: o próprio)
 * </pre>
 */
@Tag(name = "Rastreamento", description = "Posições de GPS da rota: o app envia em lote, o mapa do Suporte acompanha")
@RestController
@RequestMapping("/api/v1/rotas/{rotaId}/posicoes")
@RequiredArgsConstructor
public class PosicaoRotaController {

    private final RastreamentoService rastreamentoService;

    @Operation(summary = "Enviar posições (app — entregador)",
            description = """
                    Lote de até 500 leituras. Aceito com a rota EM_ANDAMENTO ou CONCLUIDA (sincronização atrasada).
                    Leituras repetidas (mesmo instante) são ignoradas: reenviar o lote é seguro.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Posições gravadas (resumo de novas e duplicadas)"),
            @ApiResponse(responseCode = "400", description = "Posição com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rota não encontrada (ou de outro entregador)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Rota não iniciada, lote grande demais ou hora no futuro", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<LotePosicoesResponse> registrar(@Parameter(example = "1") @PathVariable Long rotaId,
                                                          @Valid @RequestBody LotePosicoesRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rastreamentoService.registrar(rotaId, request, Solicitante.de(jwt)));
    }

    @Operation(summary = "Rastro percorrido",
            description = "Em ordem cronológica. Para atualizar o mapa sem baixar tudo de novo, passe desde = registradaEm da última posição recebida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posições"),
            @ApiResponse(responseCode = "400", description = "desde em formato inválido (ISO-8601)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rota não encontrada (ou de outro entregador)", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<PosicaoResponse>> listar(
            @Parameter(example = "1") @PathVariable Long rotaId,
            @Parameter(description = "Só posições depois deste instante (ISO-8601)", example = "2026-11-04T12:15:30Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rastreamentoService.listar(rotaId, desde, Solicitante.de(jwt)));
    }
}
