package br.com.fametro.spring.transportadora.exception.custom;

/** Conflito com o estado atual (ex.: e-mail já cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
