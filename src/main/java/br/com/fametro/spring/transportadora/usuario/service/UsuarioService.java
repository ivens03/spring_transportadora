package br.com.fametro.spring.transportadora.usuario.service;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioRequest;
import br.com.fametro.spring.transportadora.usuario.dto.response.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.UsuarioResponse;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.Vinculo;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Cadastro e manutenção de usuários ("Solicitação de Nova Matrícula" do fluxo de acesso). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RotaCertaProperties props;
    private final Clock clock;

    @Transactional
    public CodigoAtivacaoResponse criar(UsuarioRequest request) {
        Perfil perfil = definirPerfil(request.vinculo(), request.departamento());
        String email = request.email() == null || request.email().isBlank()
                ? null : Usuario.normalizarEmail(request.email());

        if (perfil.getVinculo() == Vinculo.INTERNO && email == null) {
            throw new RegraNegocioException("Funcionário precisa de e-mail corporativo (é o login do painel).");
        }
        if (email != null && usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe usuário com este e-mail.");
        }

        Instant agora = clock.instant();
        String codigo = CodigoAtivacao.gerar();
        Instant expiraEm = agora.plus(props.credencial().validadeCodigoAtivacao());
        Usuario usuario = Usuario.novo(gerarMatricula(perfil), request.nome().trim(), email, perfil,
                passwordEncoder.encode(codigo), expiraEm, agora);
        usuarioRepository.save(usuario);
        return CodigoAtivacaoResponse.de(usuario, codigo, expiraEm);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByMatriculaAsc().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorMatricula(String matricula) {
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
            throw new RegraNegocioException("Não é possível bloquear o próprio usuário.");
        }
        usuario.bloquear(clock.instant());
    }

    /** Cria o primeiro ADMINISTRATIVO (senão ninguém conseguiria cadastrar usuários). Idempotente. */
    @Transactional
    public String garantirAdministradorInicial(String nome, String email, String senha) {
        if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRATIVO)) {
            return null;
        }
        PoliticaCredencial.validar(Perfil.ADMINISTRATIVO.getTipoCredencial(), senha);
        Usuario admin = Usuario.administradorInicial(gerarMatricula(Perfil.ADMINISTRATIVO), nome,
                Usuario.normalizarEmail(email), passwordEncoder.encode(senha), clock.instant());
        usuarioRepository.save(admin);
        return admin.getMatricula();
    }

    /** Aplica as duas decisões do fluxo: "Qual o vínculo?" e, se interno, "Qual o departamento?". */
    private Perfil definirPerfil(Vinculo vinculo, Departamento departamento) {
        if (vinculo == Vinculo.EXTERNO) {
            if (departamento != null) {
                throw new RegraNegocioException("Entregador (vínculo EXTERNO) não tem departamento.");
            }
            return Perfil.ENTREGADOR;
        }
        if (departamento == null) {
            throw new RegraNegocioException("Funcionário (vínculo INTERNO) precisa informar o departamento.");
        }
        return departamento.perfil();
    }

    private Usuario carregar(String matricula) {
        return usuarioRepository.findByMatricula(Usuario.normalizarMatricula(matricula))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula));
    }

    private String gerarMatricula(Perfil perfil) {
        long numero = perfil == Perfil.ENTREGADOR
                ? usuarioRepository.proximoNumeroMatriculaEntregador()
                : usuarioRepository.proximoNumeroMatriculaFuncionario();
        return "%s-%06d".formatted(perfil.getPrefixoMatricula(), numero);
    }
}
