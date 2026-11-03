package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioPatchRequest;
import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioRequest;
import br.com.fametro.spring.transportadora.usuario.dto.response.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.UsuarioResponse;
import br.com.fametro.spring.transportadora.usuario.service.UsuarioService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Recurso usuários (permissão usuario:gerenciar = ADMINISTRATIVO, garantido no SecurityConfig):
 * <pre>
 * POST  /api/v1/usuarios                              cria (201 + Location)
 * GET   /api/v1/usuarios?page=0&amp;size=20&amp;sort=nome  lista paginada
 * GET   /api/v1/usuarios/{matricula}                  detalhe
 * PATCH /api/v1/usuarios/{matricula}                  altera nome/e-mail/status (200)
 * POST  /api/v1/usuarios/{matricula}/codigos-ativacao gera novo código (201)
 * </pre>
 */
@Tag(name = "Usuários", description = "Cadastro e manutenção de entregadores e funcionários (permissão usuario:gerenciar — ADMINISTRATIVO)")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Cadastrar usuário (nova matrícula)",
            description = """
                    vinculo EXTERNO = entregador (sem departamento, e-mail opcional).
                    vinculo INTERNO = funcionário (departamento e e-mail obrigatórios).
                    A resposta traz o código de ativação, mostrado UMA única vez.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado; header Location aponta para ele"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra de vínculo/departamento/e-mail violada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CodigoAtivacaoResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        CodigoAtivacaoResponse criado = usuarioService.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{matricula}").buildAndExpand(criado.matricula()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar usuários (paginado)",
            description = "Parâmetros: page (começa em 0), size (padrão 20, máx. 100) e sort (ex.: nome,asc). Padrão: matrícula.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários"),
            @ApiResponse(responseCode = "400", description = "Campo de ordenação inexistente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<UsuarioResponse>> listar(
            @ParameterObject @PageableDefault(sort = "matricula", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(usuarioService.listar(pageable)));
    }

    @Operation(summary = "Buscar usuário pela matrícula")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{matricula}")
    public ResponseEntity<UsuarioResponse> buscar(
            @Parameter(description = "Matrícula do usuário", example = "ENT-000001") @PathVariable String matricula) {
        return ResponseEntity.ok(usuarioService.buscarPorMatricula(matricula));
    }

    @Operation(summary = "Gerar novo código de ativação",
            description = "Esqueceu PIN/senha, trocou de celular ou desbloqueio: invalida a credencial atual e gera um código novo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Código gerado (mostrado uma única vez)"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/{matricula}/codigos-ativacao")
    public ResponseEntity<CodigoAtivacaoResponse> gerarCodigoAtivacao(
            @Parameter(description = "Matrícula do usuário", example = "ENT-000001") @PathVariable String matricula) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.reemitirCodigoAtivacao(matricula));
    }

    @Operation(summary = "Atualizar usuário (parcial)",
            description = """
                    PATCH: só os campos enviados mudam (nome, email, status).
                    status aceita apenas BLOQUEADO (repetir é inofensivo). Para reativar, gere um novo código de ativação.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já usado por outro usuário", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra violada (status diferente de BLOQUEADO, bloquear a si mesmo, funcionário sem e-mail...)", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping(path = "/{matricula}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "Matrícula do usuário", example = "ENT-000001") @PathVariable String matricula,
            @Valid @RequestBody UsuarioPatchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.atualizar(matricula, request, jwt.getSubject()));
    }
}
