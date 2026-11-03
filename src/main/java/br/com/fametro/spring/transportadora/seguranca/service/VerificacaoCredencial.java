package br.com.fametro.spring.transportadora.seguranca.service;

import java.time.Instant;

/** Resultado da conferência de uma senha/PIN contra o hash e a chave de salt ativa. */
public record VerificacaoCredencial(Situacao situacao, Instant expiraEm) {

    public enum Situacao {
        /** Senha/PIN correto e chave de salt dentro dos 30 dias. */
        VALIDA,
        /** Senha/PIN correto, mas a chave venceu: obrigatório trocar. */
        EXPIRADA,
        /** Senha/PIN errado (ou usuário sem chave ativa). */
        INVALIDA
    }

    static VerificacaoCredencial invalida() {
        return new VerificacaoCredencial(Situacao.INVALIDA, null);
    }

    public boolean correta() {
        return situacao != Situacao.INVALIDA;
    }
}
