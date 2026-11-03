package br.com.fametro.spring.transportadora.comum;

import org.springframework.http.HttpStatus;

/** Erro de negócio que vira um ProblemDetail (RFC 9457) com o status indicado. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException requisicaoInvalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    /** Mensagem sempre genérica: não revela se a matrícula/e-mail existe nem o motivo exato. */
    public static ApiException credenciaisInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }
}
