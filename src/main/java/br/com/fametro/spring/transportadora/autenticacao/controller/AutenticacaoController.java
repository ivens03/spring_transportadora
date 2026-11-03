package br.com.fametro.spring.transportadora.autenticacao.controller;

import br.com.fametro.spring.transportadora.autenticacao.dto.request.AtivacaoRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginAppRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginPainelRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaPinRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaSenhaRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.SessaoResponse;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.TokenResponse;
import br.com.fametro.spring.transportadora.autenticacao.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login de cada tipo de credencial, ativação, troca de PIN/senha e sessão atual. Rotas públicas, exceto /me. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    /** Credencial APP_ENTREGADOR: matrícula + PIN. */
    @PostMapping("/app/login")
    public ResponseEntity<TokenResponse> loginApp(@Valid @RequestBody LoginAppRequest request) {
        return ResponseEntity.ok(autenticacaoService.loginApp(request));
    }

    /** Credencial PAINEL_WEB: e-mail corporativo + senha. */
    @PostMapping("/painel/login")
    public ResponseEntity<TokenResponse> loginPainel(@Valid @RequestBody LoginPainelRequest request) {
        return ResponseEntity.ok(autenticacaoService.loginPainel(request));
    }

    /** Troca do PIN (obrigatória após 30 dias). Já devolve o token novo. */
    @PostMapping("/app/trocar-pin")
    public ResponseEntity<TokenResponse> trocarPin(@Valid @RequestBody TrocaPinRequest request) {
        return ResponseEntity.ok(autenticacaoService.trocarPin(request));
    }

    /** Troca da senha do painel (obrigatória após 30 dias). Já devolve o token novo. */
    @PostMapping("/painel/trocar-senha")
    public ResponseEntity<TokenResponse> trocarSenha(@Valid @RequestBody TrocaSenhaRequest request) {
        return ResponseEntity.ok(autenticacaoService.trocarSenha(request));
    }

    @PostMapping("/ativar")
    public ResponseEntity<Void> ativar(@Valid @RequestBody AtivacaoRequest request) {
        autenticacaoService.ativar(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<SessaoResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(autenticacaoService.sessaoAtual(jwt));
    }
}
