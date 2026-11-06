package br.com.fametro.spring.transportadora.usuario.model;

import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/** Pessoa com matrícula na transportadora (entregador ou funcionário) e sua credencial de acesso. */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String matricula;

    @Column(nullable = false)
    private String nome;

    /** Obrigatório para funcionário (é o login do painel); opcional para entregador (só contato). */
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuario status;

    /**
     * Hash Argon2id do PIN do app ou da senha do painel. Nulo até a ativação.
     * O salt fica em seguranca.chave_salt e o pepper no .env.
     */
    private String senhaHash;

    private String codigoAtivacaoHash;
    private Instant codigoAtivacaoExpiraEm;

    private int falhasLogin;
    private Instant bloqueadoAte;
    private Instant ultimoLoginEm;

    /** Só do cliente (empresa): CNPJ, telefone e endereço de entrega. Nulo para os outros perfis. */
    @Embedded
    private DadosEmpresa empresa;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public static Usuario novo(String matricula, String nome, String email, Perfil perfil,
                               String codigoAtivacaoHash, Instant codigoExpiraEm, Instant agora) {
        Usuario u = new Usuario();
        u.matricula = matricula;
        u.nome = nome;
        u.email = email;
        u.perfil = perfil;
        u.status = StatusUsuario.PENDENTE_ATIVACAO;
        u.codigoAtivacaoHash = codigoAtivacaoHash;
        u.codigoAtivacaoExpiraEm = codigoExpiraEm;
        u.criadoEm = agora;
        u.atualizadoEm = agora;
        return u;
    }

    /** Primeiro administrativo do sistema: a senha vem da configuração (.env) e é definida logo após salvar. */
    public static Usuario administradorInicial(String matricula, String nome, String email, Instant agora) {
        return novo(matricula, nome, email, Perfil.ADMINISTRATIVO, null, null, agora);
    }

    /** Matrícula sempre em maiúsculas e sem espaços (ex.: " ent-000001 " -> "ENT-000001"). */
    public static String normalizarMatricula(String matricula) {
        return matricula == null ? "" : matricula.trim().toUpperCase(Locale.ROOT);
    }

    /** E-mail sempre em minúsculas e sem espaços. */
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public TipoCredencial tipoCredencial() {
        return perfil.getTipoCredencial();
    }

    public boolean aguardandoAtivacao(Instant agora) {
        return status == StatusUsuario.PENDENTE_ATIVACAO
                && codigoAtivacaoHash != null
                && codigoAtivacaoExpiraEm != null
                && codigoAtivacaoExpiraEm.isAfter(agora);
    }

    public void ativar(String senhaHash, Instant agora) {
        this.senhaHash = senhaHash;
        this.status = StatusUsuario.ATIVO;
        this.codigoAtivacaoHash = null;
        this.codigoAtivacaoExpiraEm = null;
        this.falhasLogin = 0;
        this.bloqueadoAte = null;
        this.atualizadoEm = agora;
    }

    /** Troca de senha/PIN (voluntária ou obrigatória após 30 dias). */
    public void trocarCredencial(String novoSenhaHash, Instant agora) {
        ativar(novoSenhaHash, agora);
    }

    /** Invalida a credencial atual e volta para "aguardando ativação" (esqueceu PIN, troca de aparelho, desbloqueio). */
    public void reemitirCodigoAtivacao(String codigoAtivacaoHash, Instant expiraEm, Instant agora) {
        this.senhaHash = null;
        this.status = StatusUsuario.PENDENTE_ATIVACAO;
        this.codigoAtivacaoHash = codigoAtivacaoHash;
        this.codigoAtivacaoExpiraEm = expiraEm;
        this.falhasLogin = 0;
        this.bloqueadoAte = null;
        this.atualizadoEm = agora;
    }

    public void definirEmpresa(DadosEmpresa empresa, Instant agora) {
        this.empresa = empresa;
        this.atualizadoEm = agora;
    }

    public void alterarNome(String nome, Instant agora) {
        this.nome = nome;
        this.atualizadoEm = agora;
    }

    public void alterarEmail(String email, Instant agora) {
        this.email = email;
        this.atualizadoEm = agora;
    }

    public void bloquear(Instant agora) {
        this.status = StatusUsuario.BLOQUEADO;
        this.atualizadoEm = agora;
    }

    public boolean podeAutenticar(Instant agora) {
        return status == StatusUsuario.ATIVO
                && senhaHash != null
                && (bloqueadoAte == null || !bloqueadoAte.isAfter(agora));
    }

    /** Conta tentativa errada; ao atingir o limite, trava o login por um tempo (protege o PIN de 6 dígitos). */
    public void registrarFalhaLogin(Instant agora, int maxFalhas, Duration tempoBloqueio) {
        this.falhasLogin++;
        if (this.falhasLogin >= maxFalhas) {
            this.bloqueadoAte = agora.plus(tempoBloqueio);
            this.falhasLogin = 0;
        }
        this.atualizadoEm = agora;
    }

    public void registrarLoginComSucesso(Instant agora) {
        this.falhasLogin = 0;
        this.bloqueadoAte = null;
        this.ultimoLoginEm = agora;
        this.atualizadoEm = agora;
    }
}
