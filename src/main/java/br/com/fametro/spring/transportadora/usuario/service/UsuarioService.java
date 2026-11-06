package br.com.fametro.spring.transportadora.usuario.service;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.seguranca.service.CredencialService;
import br.com.fametro.spring.transportadora.seguranca.service.VerificacaoCredencial;
import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioPatchRequest;
import br.com.fametro.spring.transportadora.usuario.dto.request.ClienteRequest;
import br.com.fametro.spring.transportadora.usuario.model.Cnpj;
import br.com.fametro.spring.transportadora.usuario.model.DadosEmpresa;
import br.com.fametro.spring.transportadora.usuario.dto.request.UsuarioRequest;
import br.com.fametro.spring.transportadora.usuario.dto.response.CodigoAtivacaoResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.ClienteResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.EntregadorResponse;
import br.com.fametro.spring.transportadora.usuario.dto.response.UsuarioResponse;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Vinculo;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/** Cadastro e manutenção de usuários ("Solicitação de Nova Matrícula" do fluxo de acesso). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredencialService credencialService;
    private final RotaCertaProperties props;
    private final Clock clock;

    @Transactional
    public CodigoAtivacaoResponse criar(UsuarioRequest request) {
        Perfil perfil = definirPerfil(request.vinculo(), request.departamento());
        String email = request.email() == null || request.email().isBlank()
                ? null : Usuario.normalizarEmail(request.email());

        if (perfil.exigeEmail() && email == null) {
            throw new RegraNegocioException(semEmail(perfil));
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

    /**
     * Empresa cliente: cria o login do Portal do Cliente (matrícula CLI-, e-mail + senha ativada com o código
     * devolvido) e guarda CNPJ, telefone e endereço de entrega.
     */
    @Transactional
    public CodigoAtivacaoResponse criarCliente(ClienteRequest r) {
        String cnpj = Cnpj.normalizar(r.cnpj());
        if (!Cnpj.valido(cnpj)) {
            throw new RegraNegocioException("CNPJ inválido: confira os dígitos verificadores.");
        }
        if (usuarioRepository.existsByEmpresaCnpj(cnpj)) {
            throw new ConflitoException("Já existe empresa com o CNPJ " + Cnpj.formatar(cnpj) + ".");
        }
        String email = Usuario.normalizarEmail(r.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe usuário com este e-mail.");
        }

        Instant agora = clock.instant();
        String codigo = CodigoAtivacao.gerar();
        Instant expiraEm = agora.plus(props.credencial().validadeCodigoAtivacao());
        Usuario cliente = Usuario.novo(gerarMatricula(Perfil.CLIENTE), r.nome().trim(), email, Perfil.CLIENTE,
                passwordEncoder.encode(codigo), expiraEm, agora);
        cliente.definirEmpresa(new DadosEmpresa(cnpj, vazioNulo(r.telefone()), vazioNulo(r.cep()), r.endereco().trim(),
                r.bairro().trim(), r.cidade().trim(), r.uf().toUpperCase(Locale.ROOT), r.latitude(), r.longitude()), agora);
        usuarioRepository.save(cliente);
        return CodigoAtivacaoResponse.de(cliente, codigo, expiraEm);
    }

    private static String vazioNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::de);
    }

    /** Entregadores para o Suporte escolher ao montar a rota (status nulo = todos). */
    @Transactional(readOnly = true)
    public Page<EntregadorResponse> listarEntregadores(StatusUsuario status, Pageable pageable) {
        return listarPorPerfil(Perfil.ENTREGADOR, status, pageable).map(EntregadorResponse::de);
    }

    /** Clientes para o Suporte vincular à carga (status nulo = todos). */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarClientes(StatusUsuario status, Pageable pageable) {
        return listarPorPerfil(Perfil.CLIENTE, status, pageable).map(ClienteResponse::de);
    }

    private Page<Usuario> listarPorPerfil(Perfil perfil, StatusUsuario status, Pageable pageable) {
        return status == null
                ? usuarioRepository.findByPerfil(perfil, pageable)
                : usuarioRepository.findByPerfilAndStatus(perfil, status, pageable);
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
        credencialService.revogar(usuario.getId()); // a chave de salt da senha/PIN antigo deixa de valer
        return CodigoAtivacaoResponse.de(usuario, codigo, expiraEm);
    }

    /** PATCH: altera só o que veio no corpo (nome, e-mail e/ou status = BLOQUEADO). */
    @Transactional
    public UsuarioResponse atualizar(String matricula, UsuarioPatchRequest request, String matriculaSolicitante) {
        if (request.nome() == null && request.email() == null && request.status() == null) {
            throw new RegraNegocioException("Informe ao menos um campo para alterar: nome, email ou status.");
        }
        Usuario usuario = carregar(matricula);
        Instant agora = clock.instant();

        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new RegraNegocioException("O nome não pode ficar em branco.");
            }
            usuario.alterarNome(request.nome().trim(), agora);
        }
        if (request.email() != null) {
            alterarEmail(usuario, Usuario.normalizarEmail(request.email()), agora);
        }
        if (request.status() != null) {
            if (request.status() != StatusUsuario.BLOQUEADO) {
                throw new RegraNegocioException("Pelo PATCH o status só pode ir para BLOQUEADO. Para reativar, gere um novo código: "
                        + "POST /api/v1/usuarios/" + usuario.getMatricula() + "/codigos-ativacao.");
            }
            if (usuario.getMatricula().equals(matriculaSolicitante)) {
                throw new RegraNegocioException("Não é possível bloquear o próprio usuário.");
            }
            usuario.bloquear(agora);
        }
        return UsuarioResponse.de(usuario);
    }

    private void alterarEmail(Usuario usuario, String email, Instant agora) {
        if (email.isBlank()) {
            if (usuario.getPerfil().exigeEmail()) {
                throw new RegraNegocioException(semEmail(usuario.getPerfil()));
            }
            usuario.alterarEmail(null, agora);
            return;
        }
        if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe usuário com este e-mail.");
        }
        usuario.alterarEmail(email, agora);
    }

    /** Cria o primeiro ADMINISTRATIVO (senão ninguém conseguiria cadastrar usuários). Idempotente. */
    @Transactional
    public String garantirAdministradorInicial(String nome, String email, String senha) {
        if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRATIVO)) {
            return null;
        }
        PoliticaCredencial.validar(Perfil.ADMINISTRATIVO.getTipoCredencial(), senha);
        Instant agora = clock.instant();
        Usuario admin = usuarioRepository.save(Usuario.administradorInicial(
                gerarMatricula(Perfil.ADMINISTRATIVO), nome, Usuario.normalizarEmail(email), agora));
        admin.ativar(credencialService.gerar(admin.getId(), senha).hash(), agora);
        return admin.getMatricula();
    }

    private static String semEmail(Perfil perfil) {
        return perfil == Perfil.CLIENTE
                ? "Cliente precisa de e-mail (é o login do Portal do Cliente)."
                : "Funcionário precisa de e-mail corporativo (é o login do painel).";
    }

    /** Aplica as duas decisões do fluxo: "Qual o vínculo?" e, se interno, "Qual o departamento?". */
    private Perfil definirPerfil(Vinculo vinculo, Departamento departamento) {
        if (vinculo == Vinculo.EXTERNO) {
            if (departamento != null) {
                throw new RegraNegocioException("Entregador (vínculo EXTERNO) não tem departamento.");
            }
            return Perfil.ENTREGADOR;
        }
        if (vinculo == Vinculo.CLIENTE) {
            if (departamento != null) {
                throw new RegraNegocioException("Cliente (vínculo CLIENTE) não tem departamento.");
            }
            return Perfil.CLIENTE;
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

    /** O que aconteceu com uma conta fixa de teste na subida. */
    public enum SituacaoContaTeste { CRIADA, RENOVADA, PRONTA }

    /**
     * Conta fixa de teste (só fora de prod): cria se não existe; se existe, deixa sempre pronta para uso —
     * credencial igual à do .env, dentro dos 30 dias, ativa e destravada.
     */
    @Transactional
    public SituacaoContaTeste garantirContaTeste(Perfil perfil, String matricula, String nome, String email,
                                                 String credencial) {
        String m = Usuario.normalizarMatricula(matricula);
        if (!m.startsWith(perfil.getPrefixoMatricula() + "-")) {
            throw new IllegalStateException("Matrícula de teste " + m + " precisa começar com "
                    + perfil.getPrefixoMatricula() + "- (perfil " + perfil + ").");
        }
        String e = email == null || email.isBlank() ? null : Usuario.normalizarEmail(email);
        if (perfil.exigeEmail() && e == null) {
            throw new IllegalStateException("Conta de teste " + perfil + " precisa de e-mail (login do painel).");
        }
        PoliticaCredencial.validar(perfil.getTipoCredencial(), credencial);
        if (e != null) {
            usuarioRepository.findByEmail(e).filter(outro -> !outro.getMatricula().equals(m)).ifPresent(outro -> {
                throw new IllegalStateException("E-mail de teste " + e + " já pertence a " + outro.getMatricula() + ".");
            });
        }

        Instant agora = clock.instant();
        var existente = usuarioRepository.findByMatricula(m);
        if (existente.isEmpty()) {
            Usuario novo = usuarioRepository.save(Usuario.novo(m, nome, e, perfil, null, null, agora));
            novo.ativar(credencialService.gerar(novo.getId(), credencial).hash(), agora);
            return SituacaoContaTeste.CRIADA;
        }

        Usuario conta = existente.get();
        if (conta.getPerfil() != perfil) {
            throw new IllegalStateException("Matrícula de teste " + m + " já existe com perfil " + conta.getPerfil() + ".");
        }
        boolean pronta = conta.podeAutenticar(agora)
                && credencialService.verificar(conta.getId(), credencial, conta.getSenhaHash()).situacao()
                   == VerificacaoCredencial.Situacao.VALIDA;
        if (!java.util.Objects.equals(conta.getEmail(), e)) {
            conta.alterarEmail(e, agora);
        }
        if (!conta.getNome().equals(nome)) {
            conta.alterarNome(nome, agora);
        }
        if (pronta) {
            return SituacaoContaTeste.PRONTA;
        }
        conta.ativar(credencialService.gerar(conta.getId(), credencial).hash(), agora); // nova chave de 30 dias
        return SituacaoContaTeste.RENOVADA;
    }

    private String gerarMatricula(Perfil perfil) {
        long numero = switch (perfil) {
            case ENTREGADOR -> usuarioRepository.proximoNumeroMatriculaEntregador();
            case CLIENTE -> usuarioRepository.proximoNumeroMatriculaCliente();
            default -> usuarioRepository.proximoNumeroMatriculaFuncionario();
        };
        return "%s-%06d".formatted(perfil.getPrefixoMatricula(), numero);
    }
}
