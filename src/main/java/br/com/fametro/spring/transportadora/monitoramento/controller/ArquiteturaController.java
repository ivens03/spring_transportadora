package br.com.fametro.spring.transportadora.monitoramento.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import br.com.fametro.spring.transportadora.monitoramento.dto.response.ArquiteturaResponse;
import br.com.fametro.spring.transportadora.monitoramento.service.ArquiteturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <pre>
 * GET /api/v1/arquitetura    como o sistema funciona, do app à VPS (mensageria:monitorar — DESENVOLVIMENTO)
 * </pre>
 */
@Tag(name = "Monitoramento técnico", description = "Perfil DESENVOLVIMENTO: arquitetura e ambiente do servidor")
@RestController
@RequestMapping("/api/v1/arquitetura")
@RequiredArgsConstructor
public class ArquiteturaController {

    private final ArquiteturaService arquiteturaService;

    @Operation(summary = "Como funciona (arquitetura + ambiente ao vivo)",
            description = """
                    Camadas (app, painel, Traefik, API, banco, serviços externos, deploy) com frameworks e versões,
                    os fluxos de dados passo a passo, as ferramentas usadas no desenvolvimento e o ambiente medido
                    agora no servidor (Java, Spring Boot, Hibernate, Postgres, última migração, perfil).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapa e ambiente"),
            @ApiResponse(responseCode = "403", description = "Sem mensageria:monitorar", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ArquiteturaResponse> buscar() {
        return ResponseEntity.ok(arquiteturaService.montar());
    }
}
