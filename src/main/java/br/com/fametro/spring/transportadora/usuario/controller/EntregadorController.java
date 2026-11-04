package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.usuario.dto.response.EntregadorResponse;
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
 * GET /api/v1/entregadores?status=ATIVO — só leitura, para o Suporte escolher quem leva a rota
 * (o /usuarios completo continua exclusivo do Administrativo).
 */
@Tag(name = "Entregadores", description = "Consulta de entregadores para montar rotas (entrega:cadastrar ou usuario:gerenciar)")
@RestController
@RequestMapping("/api/v1/entregadores")
@RequiredArgsConstructor
public class EntregadorController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar entregadores", description = "Paginado, ordenado pelo nome. Sem status = todos.")
    @GetMapping
    public ResponseEntity<PagedModel<EntregadorResponse>> listar(
            @Parameter(example = "ATIVO") @RequestParam(required = false) StatusUsuario status,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC, size = 100) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(usuarioService.listarEntregadores(status, pageable)));
    }
}
