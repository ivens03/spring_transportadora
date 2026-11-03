package br.com.fametro.spring.transportadora.autenticacao.service;

import br.com.fametro.spring.transportadora.autenticacao.config.JwtConfig;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.AtivacaoRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginAppRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginPainelRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaPinRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaSenhaRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.SessaoResponse;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.TokenResponse;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.exception.custom.CredenciaisInvalidasException;
import br.com.fametro.spring.transportadora.exception.custom.CredencialExpiradaException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.seguranca.service.CredencialGerada;
import br.com.fametro.spring.transportadora.seguranca.service.CredencialService;
import br.com.fametro.spring.transportadora.seguranca.service.VerificacaoCredencial;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import br.com.fametro.spring.transportadora.usuario.service.CodigoAtivacao;
import br.com.fametro.spring.transportadora.usuario.service.PoliticaCredencial;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Login por tipo de credencial, ativação, troca de PIN/senha e dados da sessão atual.
 * Senha/PIN: Argon2id + salt + pepper ({@link CredencialService}); a chave de salt vence em 30 dias.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final CredencialService credencialService;
    /** Só para o código de ativação (uso único, 72h). */
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RotaCertaProperties props;
    private final Clock clock;

    // ------------------------------------------------------------------ login

    /** App do entregador: matrícula + PIN. Funcionário não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginApp(LoginAppRequest request) {
        return entrar(conferir(buscarApp(request.matricula()), request.pin(), false));
    }

    /** Painel web: e-mail corporativo + senha. Entregador não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginPainel(LoginPainelRequest request) {
        return entrar(conferir(buscarPainel(request.email()), request.senha(), false));
    }

    // ------------------------------------------------------------------ troca (obrigatória após 30 dias)

    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse trocarPin(TrocaPinRequest request) {
        Usuario usuario = conferir(buscarApp(request.matricula()), request.pinAtual(), true).usuario();
        return trocar(usuario, request.pinAtual(), request.novoPin());
    }

    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse trocarSenha(TrocaSenhaRequest request) {
        Usuario usuario = conferir(buscarPainel(request.email()), request.senhaAtual(), true).usuario();
        return trocar(usuario, request.senhaAtual(), request.novaSenha());
    }

    // ------------------------------------------------------------------ ativação

    /** Primeiro acesso: troca o código de ativação pelo PIN (app) ou senha (painel). */
    @Transactional
    public void ativar(AtivacaoRequest request) {
        Instant agora = clock.instant();
        Usuario usuario = usuarioRepository.findByMatricula(Usuario.normalizarMatricula(request.matricula()))
                .filter(u -> u.aguardandoAtivacao(agora))
                .filter(u -> passwordEncoder.matches(CodigoAtivacao.normalizar(request.codigoAtivacao()),
                        u.getCodigoAtivacaoHash()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Matrícula ou código de ativação inválido/expirado."));

        PoliticaCredencial.validar(usuario.tipoCredencial(), request.novaCredencial());
        CredencialGerada credencial = credencialService.gerar(usuario.getId(), request.novaCredencial());
        usuario.ativar(credencial.hash(), agora);
    }

    /** Dados do token atual (o front usa para montar o menu do módulo). */
    public SessaoResponse sessaoAtual(Jwt jwt) {
        Perfil perfil = Perfil.valueOf(jwt.getClaimAsString(JwtConfig.CLAIM_PERFIL));
        return new SessaoResponse(jwt.getSubject(), jwt.getClaimAsString(JwtConfig.CLAIM_NOME), perfil.name(),
                jwt.getClaimAsString(JwtConfig.CLAIM_CREDENCIAL), perfil.getModulo(),
                jwt.getClaimAsStringList(JwtConfig.CLAIM_PERMISSOES), jwt.getExpiresAt());
    }

    // ------------------------------------------------------------------ apoio

    /** Usuário com a senha/PIN já conferido e o resultado da conferência (traz o vencimento da chave). */
    private record Conferido(Usuario usuario, VerificacaoCredencial verificacao) {
    }

    private Optional<Usuario> buscarApp(String matricula) {
        return usuarioRepository.findByMatricula(Usuario.normalizarMatricula(matricula))
                .filter(u -> u.tipoCredencial() == TipoCredencial.APP_ENTREGADOR);
    }

    private Optional<Usuario> buscarPainel(String email) {
        return usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .filter(u -> u.tipoCredencial() == TipoCredencial.PAINEL_WEB);
    }

    /**
     * Confere a senha/PIN. Errada: conta tentativa e devolve 401 genérico.
     * Certa mas vencida (30 dias): no login devolve 403 CREDENCIAL_EXPIRADA; na troca é aceita.
     * O vencimento só é revelado para quem acertou a senha.
     */
    private Conferido conferir(Optional<Usuario> encontrado, String segredo, boolean aceitarExpirada) {
        Instant agora = clock.instant();
        if (encontrado.isEmpty() || !encontrado.get().podeAutenticar(agora)) {
            credencialService.simularVerificacao(segredo);
            throw new CredenciaisInvalidasException();
        }
        Usuario usuario = encontrado.get();
        VerificacaoCredencial verificacao = credencialService.verificar(usuario.getId(), segredo, usuario.getSenhaHash());
        if (!verificacao.correta()) {
            usuario.registrarFalhaLogin(agora, props.credencial().maxFalhasLogin(), props.credencial().tempoBloqueio());
            throw new CredenciaisInvalidasException();
        }
        if (verificacao.situacao() == VerificacaoCredencial.Situacao.EXPIRADA && !aceitarExpirada) {
            throw new CredencialExpiradaException(mensagemExpirada(usuario.tipoCredencial()));
        }
        return new Conferido(usuario, verificacao);
    }

    private TokenResponse entrar(Conferido conferido) {
        conferido.usuario().registrarLoginComSucesso(clock.instant());
        return tokenService.emitir(conferido.usuario(), conferido.verificacao().expiraEm());
    }

    private TokenResponse trocar(Usuario usuario, String atual, String nova) {
        PoliticaCredencial.validar(usuario.tipoCredencial(), nova);
        if (nova.equals(atual)) {
            throw new RegraNegocioException("A nova credencial precisa ser diferente da atual.");
        }
        Instant agora = clock.instant();
        CredencialGerada credencial = credencialService.gerar(usuario.getId(), nova);
        usuario.trocarCredencial(credencial.hash(), agora);
        usuario.registrarLoginComSucesso(agora);
        return tokenService.emitir(usuario, credencial.expiraEm());
    }

    private static String mensagemExpirada(TipoCredencial tipo) {
        return tipo == TipoCredencial.APP_ENTREGADOR
                ? "Seu PIN venceu (validade de 30 dias). Cadastre um novo em POST /api/auth/app/trocar-pin."
                : "Sua senha venceu (validade de 30 dias). Cadastre uma nova em POST /api/auth/painel/trocar-senha.";
    }
}
