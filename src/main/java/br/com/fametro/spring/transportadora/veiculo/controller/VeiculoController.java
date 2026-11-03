package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.veiculo.dto.request.VeiculoPatchRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.request.VeiculoRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.response.VeiculoResponse;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import br.com.fametro.spring.transportadora.veiculo.service.VeiculoService;
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
 * Recurso veículos da frota (permissão veiculo:gerenciar = ADMINISTRATIVO, SUPORTE e FINANCEIRO):
 * <pre>
 * POST  /api/v1/veiculos                          cadastra (201 + Location)
 * GET   /api/v1/veiculos?status=&amp;tipo=&amp;page=   lista paginada com filtros opcionais
 * GET   /api/v1/veiculos/{id}                     detalhe
 * PATCH /api/v1/veiculos/{id}                     altera placa, capacidades, referência, km, status (200)
 * </pre>
 */
@Tag(name = "Veículos", description = "Frota da transportadora (permissão veiculo:gerenciar — ADMINISTRATIVO, SUPORTE e FINANCEIRO)")
@RestController
@RequestMapping("/api/v1/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    @Operation(summary = "Cadastrar veículo da frota",
            description = "Placa é normalizada (maiúscula, sem hífen). Começa ATIVO. consumoReferenciaKmPorLitro habilita o alerta de consumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado; header Location aponta para ele"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Placa ou RENAVAM já cadastrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Ano de fabricação no futuro", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestBody VeiculoRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        VeiculoResponse criado = veiculoService.cadastrar(request, jwt.getSubject());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar veículos (paginado)",
            description = "Filtros opcionais status e tipo. Paginação: page, size (máx. 100), sort (ex.: placa,asc). Padrão: placa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de veículos"),
            @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<VeiculoResponse>> listar(
            @Parameter(description = "Filtra pelo status", example = "ATIVO") @RequestParam(required = false) StatusVeiculo status,
            @Parameter(description = "Filtra pelo tipo", example = "VAN") @RequestParam(required = false) TipoVeiculo tipo,
            @ParameterObject @PageableDefault(sort = "placa", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(veiculoService.listar(status, tipo, pageable)));
    }

    @Operation(summary = "Buscar veículo pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponse> buscar(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscar(id));
    }

    @Operation(summary = "Atualizar veículo (parcial)",
            description = """
                    PATCH: só os campos enviados mudam. RENAVAM, ano, tipo e combustível são fixos.
                    quilometragemAtual só aumenta. status INATIVO tira o veículo da operação (não recebe abastecimento).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo atualizado"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Placa já usada por outro veículo", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Corpo vazio, campo em branco ou quilometragem diminuindo", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<VeiculoResponse> atualizar(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long id,
            @Valid @RequestBody VeiculoPatchRequest request) {
        return ResponseEntity.ok(veiculoService.atualizar(id, request));
    }
}
