package br.com.fametro.spring.transportadora.carga.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.carga.dto.request.CargaPatchRequest;
import br.com.fametro.spring.transportadora.carga.dto.request.CargaRequest;
import br.com.fametro.spring.transportadora.carga.dto.response.CargaResponse;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.carga.service.CargaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Recurso cargas (permissão entrega:cadastrar = SUPORTE):
 * <pre>
 * POST  /api/v1/cargas                                         cadastra (201 + Location)
 * GET   /api/v1/cargas?status=AGUARDANDO_ROTA&amp;cidade=&amp;bairro=  lista paginada (escolher o que vai para a rota)
 * GET   /api/v1/cargas/{id}                                    detalhe
 * PATCH /api/v1/cargas/{id}                                    altera dados/destino ou cancela (só AGUARDANDO_ROTA)
 * </pre>
 */
@Tag(name = "Cargas", description = "Cargas a entregar; o Suporte escolhe quais entram em cada rota (permissão entrega:cadastrar — SUPORTE)")
@RestController
@RequestMapping("/api/v1/cargas")
@RequiredArgsConstructor
public class CargaController {

    private final CargaService cargaService;

    @Operation(summary = "Cadastrar carga", description = "Gera o código CRG-xxxxxx. Começa AGUARDANDO_ROTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carga cadastrada; header Location aponta para ela"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CargaResponse> cadastrar(@Valid @RequestBody CargaRequest request, @AuthenticationPrincipal Jwt jwt) {
        CargaResponse criada = cargaService.cadastrar(request, jwt.getSubject());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @Operation(summary = "Listar cargas (paginado)",
            description = "Use status=AGUARDANDO_ROTA com cidade/bairro para escolher as cargas de um destino. Padrão: mais antigas primeiro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de cargas"),
            @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<CargaResponse>> listar(
            @Parameter(example = "AGUARDANDO_ROTA") @RequestParam(required = false) StatusCarga status,
            @Parameter(example = "Fortaleza") @RequestParam(required = false) String cidade,
            @Parameter(example = "Aldeota") @RequestParam(required = false) String bairro,
            @ParameterObject @PageableDefault(sort = "criadaEm", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(cargaService.listar(status, cidade, bairro, pageable)));
    }

    @Operation(summary = "Buscar carga pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carga encontrada"),
            @ApiResponse(responseCode = "404", description = "Carga não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CargaResponse> buscar(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(cargaService.buscar(id));
    }

    @Operation(summary = "Atualizar carga (parcial)",
            description = "Só enquanto AGUARDANDO_ROTA. Destino muda em bloco (endereço + coordenadas). status aceita apenas CANCELADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carga atualizada"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carga não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Carga já roteirizada, corpo vazio, destino incompleto ou status inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<CargaResponse> atualizar(@Parameter(example = "1") @PathVariable Long id,
                                                   @Valid @RequestBody CargaPatchRequest request) {
        return ResponseEntity.ok(cargaService.atualizar(id, request));
    }
}
