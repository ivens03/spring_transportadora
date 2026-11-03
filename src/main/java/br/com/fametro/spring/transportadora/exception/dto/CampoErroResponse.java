package br.com.fametro.spring.transportadora.exception.dto;

/** Erro de validação de um campo do request. */
public record CampoErroResponse(String campo, String mensagem) {
}
