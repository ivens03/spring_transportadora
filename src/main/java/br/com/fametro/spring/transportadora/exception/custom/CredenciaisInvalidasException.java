package br.com.fametro.spring.transportadora.exception.custom;

/**
 * Login ou ativação recusados. Vira HTTP 401.
 * A mensagem padrão é genérica de propósito: não revela se a matrícula/e-mail existe nem o motivo exato.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        this("Credenciais inválidas.");
    }

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
