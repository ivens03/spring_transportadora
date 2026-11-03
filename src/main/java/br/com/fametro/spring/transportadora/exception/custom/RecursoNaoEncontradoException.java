package br.com.fametro.spring.transportadora.exception.custom;

/** Recurso pedido não existe. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
