package br.com.fametro.spring.transportadora.exception.custom;

/** Requisição válida no formato, mas que quebra uma regra de negócio. Vira HTTP 400. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
