package br.com.fametro.spring.transportadora.autenticacao.service;

import br.com.fametro.spring.transportadora.autenticacao.config.JwtConfig;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.AtivacaoRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginAppRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginPainelRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.TrocaCredencialRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.CredencialResponse;
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
 * Login por tipo de credencial, criação (ativação) e troca de credencial, e dados da sessão atual.
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

    // ------------------------------------------------------------------ tokens (login)

    /** App do entregador: matrícula + PIN. Funcionário não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginApp(LoginAppRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByMatricula(Usuario.normalizarMatricula(request.matricula()))
                .filter(u -> u.tipoCredencial() == TipoCredencial.APP_ENTREGADOR);
        return entrar(conferir(usuario, request.pin(), false));
    }

    /** Painel web: e-mail corporativo + senha. Entregador não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginPainel(LoginPainelRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(request.email()))
                .filter(u -> u.tipoCredencial() == TipoCredencial.PAINEL_WEB);
        return entrar(conferir(usuario, request.senha(), false));
    }

    // ------------------------------------------------------------------ credenciais

    /** POST /credenciais — primeiro acesso: troca o código de ativação pelo PIN (app) ou senha (painel). */
    @Transactional
    public CredencialResponse ativar(AtivacaoRequest request) {
        Instant agora = clock.instant();
        Usuario usuario = usuarioRepository.findByMatricula(Usuario.normalizarMatricula(request.matricula()))
                .filter(u -> u.aguardandoAtivacao(agora))
                .filter(u -> passwordEncoder.matches(CodigoAtivacao.normalizar(request.codigoAtivacao()),
                        u.getCodigoAtivacaoHash()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Matrícula ou código de ativação inválido/expirado."));

        PoliticaCredencial.validar(usuario.tipoCredencial(), request.credencial());
        CredencialGerada credencial = credencialService.gerar(usuario.getId(), request.credencial());
        usuario.ativar(credencial.hash(), agora);
        return new CredencialResponse(usuario.getMatricula(), usuario.tipoCredencial(), credencial.expiraEm());
    }

    /**
     * PATCH /credenciais/{matricula} — troca o PIN ou a senha (obrigatória após 30 dias, ou voluntária).
     * Exige a credencial atual (aceita mesmo vencida). Não devolve token: o cliente faz login com a nova.
     */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public CredencialResponse trocar(String matricula, TrocaCredencialRequest request) {
        Usuario usuario = conferir(usuarioRepository.findByMatricula(Usuario.normalizarMatricula(matricula)),
                request.credencialAtual(), true).usuario();

        PoliticaCredencial.validar(usuario.tipoCredencial(), request.novaCredencial());
        if (request.novaCredencial().equals(request.credencialAtual())) {
            throw new RegraNegocioException("A nova credencial precisa ser diferente da atual.");
        }
        CredencialGerada credencial = credencialService.gerar(usuario.getId(), request.novaCredencial());
        usuario.trocarCredencial(credencial.hash(), clock.instant());
        return new CredencialResponse(usuario.getMatricula(), usuario.tipoCredencial(), credencial.expiraEm());
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
            throw new CredencialExpiradaException(mensagemExpirada(usuario));
        }
        return new Conferido(usuario, verificacao);
    }

    private TokenResponse entrar(Conferido conferido) {
        conferido.usuario().registrarLoginComSucesso(clock.instant());
        return tokenService.emitir(conferido.usuario(), conferido.verificacao().expiraEm());
    }

    private static String mensagemExpirada(Usuario usuario) {
        String oQue = usuario.tipoCredencial() == TipoCredencial.APP_ENTREGADOR ? "Seu PIN venceu" : "Sua senha venceu";
        return oQue + " (validade de 30 dias). Troque em PATCH /api/v1/auth/credenciais/" + usuario.getMatricula() + ".";
    }
}
