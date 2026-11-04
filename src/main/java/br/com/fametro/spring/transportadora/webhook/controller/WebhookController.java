package br.com.fametro.spring.transportadora.webhook.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import br.com.fametro.spring.transportadora.webhook.dto.request.WebhookPatchRequest;
import br.com.fametro.spring.transportadora.webhook.dto.request.WebhookRequest;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookEntregaResponse;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookResponse;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookSegredoResponse;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import br.com.fametro.spring.transportadora.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Webhooks do Portal do Cliente (permissão webhook:gerenciar = CLIENTE). A transportadora faz POST no endereço
 * do cliente a cada mudança das encomendas dele (e a localização do veículo enquanto estão a caminho):
 * <pre>
 * POST   /api/v1/webhooks                     cadastra (201 + Location; o segredo aparece SÓ aqui)
 * GET    /api/v1/webhooks                     lista paginada
 * GET    /api/v1/webhooks/{id}                detalhe
 * PATCH  /api/v1/webhooks/{id}                url, eventos e/ou ativo (pausar)
 * DELETE /api/v1/webhooks/{id}                exclui (204)
 * POST   /api/v1/webhooks/{id}/segredos       troca o segredo (201; o antigo para de valer)
 * POST   /api/v1/webhooks/{id}/pings          manda um PING de teste (202: vai para a fila)
 * GET    /api/v1/webhooks/{id}/entregas       avisos enviados, com status HTTP e erro de cada um
 * </pre>
 */
@Tag(name = "Webhooks (Portal do Cliente)",
        description = """
                Avisos automáticos das encomendas para o servidor do cliente: eventos CADASTRADA, ROTEIRIZADA,
                SAIU_PARA_ENTREGA, LOCALIZACAO (posição do veículo + previsão, no máx. 1/min por encomenda), ENTREGUE,
                NAO_ENTREGUE, AGUARDANDO_NOVA_ROTA e CANCELADA. Assinados com HMAC-SHA256 (X-RotaCerta-Assinatura),
                reenviados com espera crescente até o servidor responder 2xx (permissão webhook:gerenciar — CLIENTE)""")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Operation(summary = "Cadastrar webhook",
            description = "Sem eventos = todos. Em produção a URL precisa ser https e pública. Guarde o segredo: ele não aparece de novo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook criado; header Location aponta para ele"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe webhook para a URL", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "URL não https/rede interna, limite atingido ou PING nos eventos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<WebhookSegredoResponse> criar(@Valid @RequestBody WebhookRequest request, @AuthenticationPrincipal Jwt jwt) {
        WebhookSegredoResponse criado = webhookService.criar(jwt.getSubject(), request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.webhook().id()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar meus webhooks (paginado)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de webhooks"),
            @ApiResponse(responseCode = "403", description = "Token sem webhook:gerenciar", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<WebhookResponse>> listar(
            @ParameterObject @PageableDefault(sort = "criadoEm", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new PagedModel<>(webhookService.listar(jwt.getSubject(), pageable)));
    }

    @Operation(summary = "Buscar webhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook encontrado"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<WebhookResponse> buscar(@Parameter(example = "1") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(webhookService.buscar(jwt.getSubject(), id));
    }

    @Operation(summary = "Atualizar webhook (parcial)", description = "url, eventos (vazio = todos) e/ou ativo=false para pausar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook atualizado"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe webhook para a URL", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Corpo vazio, URL recusada ou PING nos eventos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<WebhookResponse> atualizar(@Parameter(example = "1") @PathVariable Long id,
                                                     @Valid @RequestBody WebhookPatchRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(webhookService.atualizar(jwt.getSubject(), id, request));
    }

    @Operation(summary = "Excluir webhook", description = "Apaga também o histórico de avisos; os pendentes não são mais enviados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluído"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(example = "1") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        webhookService.excluir(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Trocar o segredo", description = "Gera um segredo novo (mostrado só nesta resposta); o antigo para de valer na hora.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Segredo novo"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/{id}/segredos")
    public ResponseEntity<WebhookSegredoResponse> trocarSegredo(@Parameter(example = "1") @PathVariable Long id,
                                                                @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webhookService.trocarSegredo(jwt.getSubject(), id));
    }

    @Operation(summary = "Testar (PING)", description = "Coloca um aviso PING na fila; acompanhe o resultado em /entregas.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "PING na fila"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Webhook pausado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/{id}/pings")
    public ResponseEntity<WebhookEntregaResponse> ping(@Parameter(example = "1") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.accepted().body(webhookService.ping(jwt.getSubject(), id));
    }

    @Operation(summary = "Avisos do webhook (paginado)",
            description = "Mais recentes primeiro. Mostra o corpo enviado, o status HTTP e o erro da última tentativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de avisos"),
            @ApiResponse(responseCode = "404", description = "Não existe ou é de outro cliente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/entregas")
    public ResponseEntity<PagedModel<WebhookEntregaResponse>> entregas(
            @Parameter(example = "1") @PathVariable Long id,
            @Parameter(example = "FALHOU") @RequestParam(required = false) StatusEntregaWebhook status,
            @ParameterObject @PageableDefault(sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new PagedModel<>(webhookService.entregas(jwt.getSubject(), id, status, pageable)));
    }
}
