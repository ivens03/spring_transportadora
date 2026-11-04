package br.com.fametro.spring.transportadora.rota.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.fametro.spring.transportadora.rota.dto.request.ParadaPatchRequest;
import br.com.fametro.spring.transportadora.rota.dto.request.RotaPatchRequest;
import br.com.fametro.spring.transportadora.rota.dto.request.RotaRequest;
import br.com.fametro.spring.transportadora.rota.dto.response.RotaResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.RotaResumoResponse;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.rota.service.RotaService;
import br.com.fametro.spring.transportadora.rota.service.Solicitante;
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
 * Recurso rotas — atende o painel (SUPORTE) e o app do entregador:
 * <pre>
 * POST  /api/v1/rotas                          gera a rota otimizada (painel, entrega:cadastrar)          201
 * GET   /api/v1/rotas?status=                  lista (painel vê todas; entregador só as dele)
 * GET   /api/v1/rotas/{id}                     rota completa: paradas, trajeto, progresso, próxima parada
 * PATCH /api/v1/rotas/{id}                     {"status":"EM_ANDAMENTO"} entregador inicia | {"status":"CANCELADA"} suporte
 * PATCH /api/v1/rotas/{id}/paradas/{ordem}     entregador baixa a parada (ENTREGUE / NAO_ENTREGUE)
 * </pre>
 */
@Tag(name = "Rotas", description = """
        Roteirização e execução: o Suporte gera a rota a partir das cargas escolhidas; o entregador inicia,
        acompanha a próxima parada e baixa as entregas. Rota de outro entregador responde 404.""")
@RestController
@RequestMapping("/api/v1/rotas")
@RequiredArgsConstructor
public class RotaController {

    private final RotaService rotaService;

    @Operation(summary = "Gerar rota (painel — SUPORTE)",
            description = """
                    Confere veículo ATIVO e livre, entregador ATIVO e livre, cargas AGUARDANDO_ROTA e capacidade
                    (peso e volume). Define a ordem das paradas (vizinho mais próximo + 2-opt) saindo do depósito
                    ou da origem informada e estima distância e duração. As cargas passam a ROTEIRIZADA.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rota gerada (PLANEJADA); header Location aponta para ela"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Veículo, entregador ou carga não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Veículo ou entregador já numa rota ativa", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Capacidade excedida, carga indisponível, veículo/entregador inapto", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RotaResponse> gerar(@Valid @RequestBody RotaRequest request, @AuthenticationPrincipal Jwt jwt) {
        RotaResponse criada = rotaService.gerar(request, jwt.getSubject());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @Operation(summary = "Listar rotas (paginado)",
            description = "Painel vê todas; o entregador (app) vê só as dele. Ex.: status=PLANEJADA para o app saber o que tem para hoje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de rotas"),
            @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<RotaResumoResponse>> listar(
            @Parameter(example = "EM_ANDAMENTO") @RequestParam(required = false) StatusRota status,
            @ParameterObject @PageableDefault(sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new PagedModel<>(rotaService.listar(status, Solicitante.de(jwt), pageable)));
    }

    @Operation(summary = "Rota completa (o que a tela desenha)",
            description = "Paradas em ordem, trajeto planejado, progresso, próxima parada, última posição e distância/minutos até a próxima.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rota"),
            @ApiResponse(responseCode = "404", description = "Rota não encontrada (ou de outro entregador)", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RotaResponse> buscar(@Parameter(example = "1") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rotaService.buscar(id, Solicitante.de(jwt)));
    }

    @Operation(summary = "Iniciar (app) ou cancelar (painel) a rota",
            description = """
                    Entregador: {"status":"EM_ANDAMENTO"} inicia — cargas vão para EM_TRANSITO e a resposta já traz a
                    rota para a tela. Suporte: {"status":"CANCELADA"} só antes de sair — cargas voltam para a fila.
                    Repetir o status atual devolve 200 sem mudar nada (reenvio do app).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rota atualizada"),
            @ApiResponse(responseCode = "404", description = "Rota não encontrada (ou de outro entregador)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Transição não permitida para o status atual ou para quem chamou", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<RotaResponse> atualizar(@Parameter(example = "1") @PathVariable Long id,
                                                  @Valid @RequestBody RotaPatchRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rotaService.atualizar(id, request, Solicitante.de(jwt)));
    }

    @Operation(summary = "Baixar parada (app — entregador)",
            description = """
                    ENTREGUE ou NAO_ENTREGUE (motivo obrigatório). Pode baixar fora da ordem. Na última baixa a rota
                    vira CONCLUIDA e as cargas não entregues voltam para AGUARDANDO_ROTA (tentativa contada).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parada baixada; devolve a rota atualizada (próxima parada)"),
            @ApiResponse(responseCode = "404", description = "Rota ou parada não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Parada já baixada com outro resultado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Rota não está EM_ANDAMENTO, motivo ausente ou hora no futuro", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{id}/paradas/{ordem}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<RotaResponse> baixarParada(@Parameter(example = "1") @PathVariable Long id,
                                                     @Parameter(description = "Ordem da parada na rota", example = "1") @PathVariable int ordem,
                                                     @Valid @RequestBody ParadaPatchRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rotaService.baixarParada(id, ordem, request, Solicitante.de(jwt)));
    }
}
