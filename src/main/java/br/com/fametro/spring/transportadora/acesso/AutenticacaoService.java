package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import br.com.fametro.spring.transportadora.acesso.dominio.Usuario;
import br.com.fametro.spring.transportadora.acesso.dominio.UsuarioRepository;
import br.com.fametro.spring.transportadora.acesso.web.TokenResponse;
import br.com.fametro.spring.transportadora.comum.ApiException;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Login por tipo de credencial e ativação da matrícula. */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RotaCertaProperties props;
    private final Clock clock;
    /** Usado para gastar o mesmo tempo quando o usuário não existe (não vaza quem existe pelo tempo de resposta). */
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService,
                               RotaCertaProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.props = props;
        this.clock = clock;
        this.hashFicticio = passwordEncoder.encode("rotacerta-hash-ficticio");
    }

    /** App do entregador: matrícula + PIN. Funcionário não entra por aqui. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse loginApp(String matricula, String pin) {
        Optional<Usuario> usuario = usuarios.findByMatricula(normalizarMatricula(matricula))
                .filter(u -> u.tipoCredencial() == TipoCredencial.APP_ENTREGADOR);
        return autenticar(usuario, pin);
    }

    /** Painel web: e-mail corporativo + senha. Entregador não entra por aqui. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse loginPainel(String email, String senha) {
        Optional<Usuario> usuario = usuarios.findByEmail(normalizarEmail(email))
                .filter(u -> u.tipoCredencial() == TipoCredencial.PAINEL_WEB);
        return autenticar(usuario, senha);
    }

    /** Primeiro acesso: troca o código de ativação pelo PIN (app) ou senha (painel). */
    @Transactional
    public void ativar(String matricula, String codigoAtivacao, String novaCredencial) {
        Instant agora = clock.instant();
        Usuario usuario = usuarios.findByMatricula(normalizarMatricula(matricula))
                .filter(u -> u.aguardandoAtivacao(agora))
                .filter(u -> passwordEncoder.matches(CodigoAtivacao.normalizar(codigoAtivacao), u.getCodigoAtivacaoHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Matrícula ou código de ativação inválido/expirado."));

        PoliticaCredencial.validar(usuario.tipoCredencial(), novaCredencial);
        usuario.ativar(passwordEncoder.encode(novaCredencial), agora);
    }

    private TokenResponse autenticar(Optional<Usuario> encontrado, String segredo) {
        Instant agora = clock.instant();
        String segredoInformado = segredo == null ? "" : segredo;

        if (encontrado.isEmpty() || !encontrado.get().podeAutenticar(agora)) {
            passwordEncoder.matches(segredoInformado, hashFicticio);
            throw ApiException.credenciaisInvalidas();
        }
        Usuario usuario = encontrado.get();
        if (!passwordEncoder.matches(segredoInformado, usuario.getSenhaHash())) {
            usuario.registrarFalhaLogin(agora, props.credencial().maxFalhasLogin(), props.credencial().tempoBloqueio());
            throw ApiException.credenciaisInvalidas();
        }
        usuario.registrarLoginComSucesso(agora);
        return tokenService.emitir(usuario);
    }

    static String normalizarMatricula(String matricula) {
        return matricula == null ? "" : matricula.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
