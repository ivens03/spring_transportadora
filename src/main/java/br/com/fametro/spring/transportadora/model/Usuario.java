package br.com.fametro.spring.transportadora.model;

import br.com.fametro.spring.transportadora.model.enums.Perfil;
import br.com.fametro.spring.transportadora.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.model.enums.TipoCredencial;
import jakarta.persistence.Column;
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

    /** Hash (BCrypt) do PIN do app ou da senha do painel. Nulo até a ativação. */
    private String senhaHash;

    private String codigoAtivacaoHash;
    private Instant codigoAtivacaoExpiraEm;

    private int falhasLogin;
    private Instant bloqueadoAte;
    private Instant ultimoLoginEm;

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

    /** Primeiro administrativo do sistema: já nasce ativo (senha vem da configuração). */
    public static Usuario administradorInicial(String matricula, String nome, String email, String senhaHash, Instant agora) {
        Usuario u = novo(matricula, nome, email, Perfil.ADMINISTRATIVO, null, null, agora);
        u.ativar(senhaHash, agora);
        return u;
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
