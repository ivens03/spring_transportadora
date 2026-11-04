package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.usuario.dto.response.ClienteResponse;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/clientes?status=ATIVO — só leitura, para o Suporte escolher o cliente (dono da encomenda) ao
 * cadastrar a carga. O cadastro do cliente é do Administrativo: POST /api/v1/usuarios com vinculo CLIENTE.
 */
@Tag(name = "Clientes", description = "Consulta de clientes para vincular às cargas (entrega:cadastrar ou usuario:gerenciar)")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar clientes", description = "Paginado, ordenado pelo nome. Sem status = todos.")
    @GetMapping
    public ResponseEntity<PagedModel<ClienteResponse>> listar(
            @Parameter(example = "ATIVO") @RequestParam(required = false) StatusUsuario status,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC, size = 100) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(usuarioService.listarClientes(status, pageable)));
    }
}
