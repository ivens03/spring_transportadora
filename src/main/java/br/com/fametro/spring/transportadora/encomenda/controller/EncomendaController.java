package br.com.fametro.spring.transportadora.encomenda.controller;

import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResumoResponse;
import br.com.fametro.spring.transportadora.encomenda.service.EncomendaService;
import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal do Cliente (permissão encomenda:acompanhar = CLIENTE). Só as encomendas do cliente do token:
 * <pre>
 * GET /api/v1/encomendas?status=EM_TRANSITO   lista paginada (mais recentes primeiro)
 * GET /api/v1/encomendas/{codigo}             detalhe: rota, posição do veículo, previsão e linha do tempo
 * </pre>
 * Para ser avisado sem ficar consultando: /api/v1/webhooks.
 */
@Tag(name = "Encomendas (Portal do Cliente)",
        description = "O cliente acompanha as próprias encomendas: status, onde está o veículo e a previsão (permissão encomenda:acompanhar — CLIENTE)")
@RestController
@RequestMapping("/api/v1/encomendas")
@RequiredArgsConstructor
public class EncomendaController {

    private final EncomendaService encomendaService;

    @Operation(summary = "Listar minhas encomendas (paginado)", description = "Padrão: mais recentes primeiro. Sem status = todas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de encomendas"),
            @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Token sem encomenda:acompanhar", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EncomendaResumoResponse>> listar(
            @Parameter(example = "EM_TRANSITO") @RequestParam(required = false) StatusCarga status,
            @ParameterObject @PageableDefault(sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new PagedModel<>(encomendaService.listar(jwt.getSubject(), status, pageable)));
    }

    @Operation(summary = "Rastrear encomenda",
            description = """
                    Status, rota, última posição do veículo, quantas entregas faltam antes da sua, distância e previsão
                    de chegada (estimativa: linha reta × fator viário ÷ velocidade média + tempo das entregas anteriores),
                    mais a linha do tempo. Localização e previsão só com a encomenda a caminho.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encomenda encontrada"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<EncomendaResponse> buscar(@Parameter(example = "CRG-000005") @PathVariable String codigo,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(encomendaService.buscar(jwt.getSubject(), codigo));
    }
}
