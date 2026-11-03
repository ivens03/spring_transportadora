package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioRequest;
import br.com.fametro.spring.transportadora.usuario.dto.response.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.UsuarioResponse;
import br.com.fametro.spring.transportadora.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

/** Módulo Gestão & Cadastros Base: só ADMINISTRATIVO (garantido no SecurityConfig). */
@RestController
@RequestMapping("/api/painel/gestao/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<CodigoAtivacaoResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        CodigoAtivacaoResponse criado = usuarioService.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{matricula}").buildAndExpand(criado.matricula()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{matricula}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable String matricula) {
        return ResponseEntity.ok(usuarioService.buscarPorMatricula(matricula));
    }

    /** Esqueceu PIN/senha, trocou de celular ou desbloqueio. */
    @PostMapping("/{matricula}/codigo-ativacao")
    public ResponseEntity<CodigoAtivacaoResponse> reemitirCodigoAtivacao(@PathVariable String matricula) {
        return ResponseEntity.ok(usuarioService.reemitirCodigoAtivacao(matricula));
    }

    @PostMapping("/{matricula}/bloqueio")
    public ResponseEntity<Void> bloquear(@PathVariable String matricula, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.bloquear(matricula, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
