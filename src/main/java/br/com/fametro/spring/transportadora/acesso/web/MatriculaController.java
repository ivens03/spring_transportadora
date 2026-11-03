package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.MatriculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Módulo Gestão & Cadastros Base (só ADMINISTRATIVO, garantido no SecurityConfig). */
@RestController
@RequestMapping("/api/painel/gestao/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodigoAtivacaoResponse criar(@Valid @RequestBody NovaMatriculaRequest req) {
        return matriculaService.criar(req);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return matriculaService.listar();
    }

    @GetMapping("/{matricula}")
    public UsuarioResponse buscar(@PathVariable String matricula) {
        return matriculaService.buscar(matricula);
    }

    @PostMapping("/{matricula}/codigo-ativacao")
    public CodigoAtivacaoResponse reemitirCodigoAtivacao(@PathVariable String matricula) {
        return matriculaService.reemitirCodigoAtivacao(matricula);
    }

    @PostMapping("/{matricula}/bloqueio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bloquear(@PathVariable String matricula, @AuthenticationPrincipal Jwt jwt) {
        matriculaService.bloquear(matricula, jwt.getSubject());
    }
}
