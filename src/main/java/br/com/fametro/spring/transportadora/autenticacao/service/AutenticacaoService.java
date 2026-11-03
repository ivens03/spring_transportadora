package br.com.fametro.spring.transportadora.autenticacao.service;

import br.com.fametro.spring.transportadora.autenticacao.config.JwtConfig;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.AtivacaoRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginAppRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.request.LoginPainelRequest;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.SessaoResponse;
import br.com.fametro.spring.transportadora.autenticacao.dto.response.TokenResponse;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.exception.custom.CredenciaisInvalidasException;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import br.com.fametro.spring.transportadora.usuario.service.CodigoAtivacao;
import br.com.fametro.spring.transportadora.usuario.service.PoliticaCredencial;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Login por tipo de credencial, ativação do usuário e dados da sessão atual. */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RotaCertaProperties props;
    private final Clock clock;
    /** Usado para gastar o mesmo tempo quando o usuário não existe (não vaza quem existe pelo tempo de resposta). */
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                               TokenService tokenService, RotaCertaProperties props, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.props = props;
        this.clock = clock;
        this.hashFicticio = passwordEncoder.encode("rotacerta-hash-ficticio");
    }

    /** App do entregador: matrícula + PIN. Funcionário não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginApp(LoginAppRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByMatricula(Usuario.normalizarMatricula(request.matricula()))
                .filter(u -> u.tipoCredencial() == TipoCredencial.APP_ENTREGADOR);
        return autenticar(usuario, request.pin());
    }

    /** Painel web: e-mail corporativo + senha. Entregador não entra por aqui. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse loginPainel(LoginPainelRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(request.email()))
                .filter(u -> u.tipoCredencial() == TipoCredencial.PAINEL_WEB);
        return autenticar(usuario, request.senha());
    }

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
        usuario.ativar(passwordEncoder.encode(request.novaCredencial()), agora);
    }

    /** Dados do token atual (o front usa para montar o menu do módulo). */
    public SessaoResponse sessaoAtual(Jwt jwt) {
        Perfil perfil = Perfil.valueOf(jwt.getClaimAsString(JwtConfig.CLAIM_PERFIL));
        return new SessaoResponse(jwt.getSubject(), jwt.getClaimAsString(JwtConfig.CLAIM_NOME), perfil.name(),
                jwt.getClaimAsString(JwtConfig.CLAIM_CREDENCIAL), perfil.getModulo(),
                jwt.getClaimAsStringList(JwtConfig.CLAIM_PERMISSOES), jwt.getExpiresAt());
    }

    private TokenResponse autenticar(Optional<Usuario> encontrado, String segredo) {
        Instant agora = clock.instant();
        String segredoInformado = segredo == null ? "" : segredo;

        if (encontrado.isEmpty() || !encontrado.get().podeAutenticar(agora)) {
            passwordEncoder.matches(segredoInformado, hashFicticio);
            throw new CredenciaisInvalidasException();
        }
        Usuario usuario = encontrado.get();
        if (!passwordEncoder.matches(segredoInformado, usuario.getSenhaHash())) {
            usuario.registrarFalhaLogin(agora, props.credencial().maxFalhasLogin(), props.credencial().tempoBloqueio());
            throw new CredenciaisInvalidasException();
        }
        usuario.registrarLoginComSucesso(agora);
        return tokenService.emitir(usuario);
    }
}
