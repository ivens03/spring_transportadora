package br.com.fametro.spring.transportadora.seguranca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Chave de salt usada no hash Argon2id da senha/PIN de um usuário (schema {@code seguranca}).
 * Vale 30 dias: vencida, o usuário é obrigado a trocar a senha/PIN, o que gera uma chave nova.
 * Guarda só o {@code usuarioId} (sem relacionamento JPA) para o módulo seguranca não depender do módulo usuario.
 */
@Entity
@Table(schema = "seguranca", name = "chave_salt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChaveSalt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long usuarioId;

    /** Salt aleatório em Base64. */
    @Column(nullable = false, updatable = false)
    private String salt;

    /** Marca a chave em uso. As anteriores ficam desativadas, como histórico. */
    @Column(nullable = false)
    private boolean ativa;

    @Column(nullable = false, updatable = false)
    private Instant cadastradaEm;

    @Column(nullable = false, updatable = false)
    private Instant expiraEm;

    private Instant desativadaEm;

    public static ChaveSalt nova(Long usuarioId, byte[] salt, Instant agora, Duration validade) {
        ChaveSalt chave = new ChaveSalt();
        chave.usuarioId = usuarioId;
        chave.salt = Base64.getEncoder().encodeToString(salt);
        chave.ativa = true;
        chave.cadastradaEm = agora;
        chave.expiraEm = agora.plus(validade);
        return chave;
    }

    public byte[] saltEmBytes() {
        return Base64.getDecoder().decode(salt);
    }

    public boolean vencida(Instant agora) {
        return !expiraEm.isAfter(agora);
    }

    public void desativar(Instant agora) {
        this.ativa = false;
        this.desativadaEm = agora;
    }
}
