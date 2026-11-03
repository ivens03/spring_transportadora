package br.com.fametro.spring.transportadora.veiculo.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.veiculo.dto.request.AbastecimentoRequest;
import br.com.fametro.spring.transportadora.veiculo.dto.response.AbastecimentoResponse;
import br.com.fametro.spring.transportadora.veiculo.service.AbastecimentoService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Sub-recurso abastecimentos do veículo (permissão abastecimento:registrar = ADMINISTRATIVO, SUPORTE e FINANCEIRO):
 * <pre>
 * POST /api/v1/veiculos/{veiculoId}/abastecimentos        registra (201 + Location)
 * GET  /api/v1/veiculos/{veiculoId}/abastecimentos        lista paginada (mais recente primeiro)
 * GET  /api/v1/veiculos/{veiculoId}/abastecimentos/{id}   detalhe
 * </pre>
 * Abastecimento não tem PATCH: é registro contábil; lançamento errado se corrige com novo lançamento.
 */
@Tag(name = "Abastecimentos", description = "Abastecimentos da frota, base das métricas de consumo (permissão abastecimento:registrar — ADMINISTRATIVO, SUPORTE e FINANCEIRO)")
@RestController
@RequestMapping("/api/v1/veiculos/{veiculoId}/abastecimentos")
@RequiredArgsConstructor
public class AbastecimentoController {

    private final AbastecimentoService abastecimentoService;

    @Operation(summary = "Registrar abastecimento (tanque cheio)",
            description = """
                    O odômetro precisa ficar entre o do abastecimento anterior e o do seguinte (por data), então
                    lançamentos atrasados são aceitos. FLEX aceita GASOLINA ou ETANOL. Veículo INATIVO não recebe abastecimento.
                    Atualiza a quilometragem do veículo se o odômetro for maior.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Abastecimento registrado; header Location aponta para ele"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido ou data no futuro", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe abastecimento do veículo nessa data/hora", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Odômetro incoerente, combustível incompatível ou veículo inativo", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AbastecimentoResponse> registrar(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long veiculoId,
            @Valid @RequestBody AbastecimentoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AbastecimentoResponse criado = abastecimentoService.registrar(veiculoId, request, jwt.getSubject());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar abastecimentos do veículo (paginado)",
            description = "Padrão: dataHora decrescente. Aceita page, size (máx. 100) e sort.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de abastecimentos"),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<AbastecimentoResponse>> listar(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long veiculoId,
            @ParameterObject @PageableDefault(sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(abastecimentoService.listar(veiculoId, pageable)));
    }

    @Operation(summary = "Buscar abastecimento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Abastecimento encontrado"),
            @ApiResponse(responseCode = "404", description = "Abastecimento (ou veículo) não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AbastecimentoResponse> buscar(
            @Parameter(description = "Id do veículo", example = "1") @PathVariable Long veiculoId,
            @Parameter(description = "Id do abastecimento", example = "10") @PathVariable Long id) {
        return ResponseEntity.ok(abastecimentoService.buscar(veiculoId, id));
    }
}
