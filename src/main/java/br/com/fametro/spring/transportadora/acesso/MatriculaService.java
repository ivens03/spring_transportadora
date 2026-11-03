package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import br.com.fametro.spring.transportadora.acesso.dominio.Usuario;
import br.com.fametro.spring.transportadora.acesso.dominio.UsuarioRepository;
import br.com.fametro.spring.transportadora.acesso.dominio.Vinculo;
import br.com.fametro.spring.transportadora.acesso.web.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.acesso.web.NovaMatriculaRequest;
import br.com.fametro.spring.transportadora.acesso.web.UsuarioResponse;
import br.com.fametro.spring.transportadora.comum.ApiException;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** "Solicitação de Nova Matrícula": decide o perfil pelo fluxo e gera a credencial inicial. */
@Service
public class MatriculaService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final RotaCertaProperties props;
    private final Clock clock;

    public MatriculaService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                            RotaCertaProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public CodigoAtivacaoResponse criar(NovaMatriculaRequest req) {
        Perfil perfil = Perfil.de(req.vinculo(), req.departamento());
        String email = req.email() == null || req.email().isBlank() ? null : AutenticacaoService.normalizarEmail(req.email());

        if (perfil.getVinculo() == Vinculo.INTERNO && email == null) {
            throw ApiException.requisicaoInvalida("Funcionário precisa de e-mail corporativo (é o login do painel).");
        }
        if (email != null && usuarios.existsByEmail(email)) {
            throw ApiException.conflito("Já existe matrícula com este e-mail.");
        }

        Instant agora = clock.instant();
        String codigo = CodigoAtivacao.gerar();
        Instant expiraEm = agora.plus(props.credencial().validadeCodigoAtivacao());
        Usuario usuario = Usuario.novaMatricula(gerarMatricula(perfil), req.nome().trim(), email, perfil,
                passwordEncoder.encode(codigo), expiraEm, agora);
        usuarios.save(usuario);
        return CodigoAtivacaoResponse.de(usuario, codigo, expiraEm);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByMatriculaAsc().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(String matricula) {
        return UsuarioResponse.de(carregar(matricula));
    }

    /** Esqueceu PIN/senha, trocou de celular ou foi desbloqueado: invalida a credencial e gera novo código. */
    @Transactional
    public CodigoAtivacaoResponse reemitirCodigoAtivacao(String matricula) {
        Usuario usuario = carregar(matricula);
        Instant agora = clock.instant();
        String codigo = CodigoAtivacao.gerar();
        Instant expiraEm = agora.plus(props.credencial().validadeCodigoAtivacao());
        usuario.reemitirCodigoAtivacao(passwordEncoder.encode(codigo), expiraEm, agora);
        return CodigoAtivacaoResponse.de(usuario, codigo, expiraEm);
    }

    @Transactional
    public void bloquear(String matricula, String matriculaSolicitante) {
        Usuario usuario = carregar(matricula);
        if (usuario.getMatricula().equals(matriculaSolicitante)) {
            throw ApiException.requisicaoInvalida("Não é possível bloquear a própria matrícula.");
        }
        usuario.bloquear(clock.instant());
    }

    /** Cria o primeiro ADMINISTRATIVO (senão ninguém conseguiria criar matrículas). Idempotente. */
    @Transactional
    public String garantirAdministradorInicial(String nome, String email, String senha) {
        if (usuarios.existsByPerfil(Perfil.ADMINISTRATIVO)) {
            return null;
        }
        PoliticaCredencial.validar(Perfil.ADMINISTRATIVO.getTipoCredencial(), senha);
        Usuario admin = Usuario.administradorInicial(gerarMatricula(Perfil.ADMINISTRATIVO), nome,
                AutenticacaoService.normalizarEmail(email), passwordEncoder.encode(senha), clock.instant());
        usuarios.save(admin);
        return admin.getMatricula();
    }

    private Usuario carregar(String matricula) {
        return usuarios.findByMatricula(AutenticacaoService.normalizarMatricula(matricula))
                .orElseThrow(() -> ApiException.naoEncontrado("Matrícula não encontrada."));
    }

    private String gerarMatricula(Perfil perfil) {
        long numero = perfil == Perfil.ENTREGADOR
                ? usuarios.proximoNumeroMatriculaEntregador()
                : usuarios.proximoNumeroMatriculaFuncionario();
        return "%s-%06d".formatted(perfil.getPrefixoMatricula(), numero);
    }
}
