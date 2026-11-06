package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import br.com.fametro.spring.transportadora.usuario.dto.request.ClienteRequest;
import br.com.fametro.spring.transportadora.usuario.dto.response.ClienteResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Empresas clientes (perfil CLIENTE):
 * <pre>
 * GET  /api/v1/clientes?status=ATIVO   lista (com CNPJ e endereço de entrega) para o Suporte escolher na nova entrega
 * POST /api/v1/clientes                cadastra a empresa (cliente:cadastrar — ADMINISTRATIVO e SUPORTE) e devolve o
 *                                      código de ativação do login do Portal do Cliente
 * </pre>
 */
@Tag(name = "Clientes (empresas)", description = "Empresas clientes: cadastro (cliente:cadastrar) e consulta para vincular às entregas (entrega:cadastrar ou usuario:gerenciar)")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar empresas clientes", description = "Paginado, ordenado pelo nome. Sem status = todas.")
    @GetMapping
    public ResponseEntity<PagedModel<ClienteResponse>> listar(
            @Parameter(example = "ATIVO") @RequestParam(required = false) StatusUsuario status,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC, size = 100) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(usuarioService.listarClientes(status, pageable)));
    }

    @Operation(summary = "Cadastrar empresa cliente",
            description = """
                    Cria a empresa (CNPJ, telefone, endereço de entrega) e o login dela no Portal do Cliente (matrícula
                    CLI-, entra com o e-mail). Devolve UMA vez o código de ativação, que a empresa usa para criar a senha.
                    O CNPJ pode vir com ou sem pontuação e já aceita o formato alfanumérico.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada"),
            @ApiResponse(responseCode = "400", description = "Campo com formato inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "CNPJ ou e-mail já cadastrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "422", description = "CNPJ com dígito verificador errado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CodigoAtivacaoResponse> cadastrar(@Valid @RequestBody ClienteRequest request) {
        CodigoAtivacaoResponse criado = usuarioService.criarCliente(request);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + criado.matricula())).body(criado);
    }
}
