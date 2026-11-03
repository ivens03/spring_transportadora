package br.com.fametro.spring.transportadora.autenticacao.controller;

import br.com.fametro.spring.transportadora.autenticacao.dto.request.AtivacaoRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginAppRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginPainelRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaCredencialRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.CredencialResponse;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.SessaoResponse;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.TokenResponse;
import br.com.fametro.spring.transportadora.autenticacao.service.AutenticacaoService;
import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recursos de autenticação (RESTful):
 * <pre>
 * POST  /api/v1/auth/app/tokens             cria token do app (login do entregador)
 * POST  /api/v1/auth/painel/tokens          cria token do painel (login do funcionário)
 * POST  /api/v1/auth/credenciais            cria a credencial (ativação: código -> PIN/senha)
 * PATCH /api/v1/auth/credenciais/{matricula} troca PIN/senha (obrigatório a cada 30 dias)
 * GET   /api/v1/auth/sessao                 dados do token atual
 * </pre>
 */
@Tag(name = "Autenticação", description = "Tokens (login) por tipo de credencial, credenciais (ativação e troca de PIN/senha) e sessão atual")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @Operation(summary = "Login do entregador (app)",
            description = "Credencial APP_ENTREGADOR: matrícula + PIN de 6 dígitos. Token vale 12h e só acessa /api/v1/app/**.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token criado"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (ou login travado após 5 erros)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "PIN vencido (30 dias): codigo = CREDENCIAL_EXPIRADA", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/app/tokens")
    public ResponseEntity<TokenResponse> criarTokenApp(@Valid @RequestBody LoginAppRequest request) {
        return ResponseEntity.ok(autenticacaoService.loginApp(request));
    }

    @Operation(summary = "Login do funcionário (painel web)",
            description = "Credencial PAINEL_WEB: e-mail corporativo + senha. Token vale 8h e só acessa o módulo do departamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token criado"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (ou login travado após 5 erros)", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Senha vencida (30 dias): codigo = CREDENCIAL_EXPIRADA", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/painel/tokens")
    public ResponseEntity<TokenResponse> criarTokenPainel(@Valid @RequestBody LoginPainelRequest request) {
        return ResponseEntity.ok(autenticacaoService.loginPainel(request));
    }

    @Operation(summary = "Criar credencial (ativação / primeiro acesso)",
            description = "Troca o código de ativação (8 caracteres, uso único, 72h) pelo PIN (entregador) ou senha (funcionário).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credencial criada; usuário ativo"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Matrícula ou código inválido/expirado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "PIN/senha fora da política", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/credenciais")
    public ResponseEntity<CredencialResponse> criarCredencial(@Valid @RequestBody AtivacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacaoService.ativar(request));
    }

    @Operation(summary = "Trocar PIN ou senha",
            description = """
                    Obrigatório quando vence (30 dias, login devolve 403 CREDENCIAL_EXPIRADA); também pode ser voluntário.
                    Vale para entregador (PIN) e funcionário (senha). Gera nova chave de salt. Depois, faça login com a nova.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credencial trocada"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credencial atual incorreta", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Nova credencial fora da política ou igual à atual", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PatchMapping(path = "/credenciais/{matricula}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<CredencialResponse> trocarCredencial(
            @Parameter(description = "Matrícula do usuário", example = "ENT-000001") @PathVariable String matricula,
            @Valid @RequestBody TrocaCredencialRequest request) {
        return ResponseEntity.ok(autenticacaoService.trocar(matricula, request));
    }

    @Operation(summary = "Sessão atual", description = "Quem é o dono do token: perfil, módulo e permissões (o front monta o menu com isso).")
    @ApiResponse(responseCode = "200", description = "Dados da sessão")
    @GetMapping("/sessao")
    public ResponseEntity<SessaoResponse> sessao(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(autenticacaoService.sessaoAtual(jwt));
    }
}
