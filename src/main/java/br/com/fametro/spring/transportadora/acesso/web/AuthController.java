package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.AutenticacaoService;
import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    /** Credencial APP_ENTREGADOR: matrícula + PIN. */
    @PostMapping("/app/login")
    public TokenResponse loginApp(@Valid @RequestBody LoginAppRequest req) {
        return autenticacaoService.loginApp(req.matricula(), req.pin());
    }

    /** Credencial PAINEL_WEB: e-mail corporativo + senha. */
    @PostMapping("/painel/login")
    public TokenResponse loginPainel(@Valid @RequestBody LoginPainelRequest req) {
        return autenticacaoService.loginPainel(req.email(), req.senha());
    }

    @PostMapping("/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@Valid @RequestBody AtivacaoRequest req) {
        autenticacaoService.ativar(req.matricula(), req.codigoAtivacao(), req.novaCredencial());
    }

    @GetMapping("/me")
    public SessaoResponse me(@AuthenticationPrincipal Jwt jwt) {
        Perfil perfil = Perfil.valueOf(jwt.getClaimAsString("perfil"));
        return new SessaoResponse(jwt.getSubject(), jwt.getClaimAsString("nome"), perfil.name(),
                jwt.getClaimAsString("cred"), perfil.getModulo(), jwt.getClaimAsStringList("permissoes"),
                jwt.getExpiresAt());
    }
}
