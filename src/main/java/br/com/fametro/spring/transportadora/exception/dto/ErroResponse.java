package br.com.fametro.spring.transportadora.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padrão de TODO erro da API (negócio, validação, 401/403 da segurança e erro inesperado).
 * {@code codigo} só aparece quando o cliente precisa reagir de forma específica (ex.: CREDENCIAL_EXPIRADA).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(Instant timestamp, int status, String erro, String codigo, String mensagem, String caminho,
                           List<CampoErroResponse> campos) {

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), null, mensagem, caminho, List.of());
    }

    public static ErroResponse de(HttpStatus status, String codigo, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo, mensagem, caminho, List.of());
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoErroResponse> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), null, mensagem, caminho, campos);
    }
}
