package br.com.fametro.spring.transportadora.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Corpo padrão de TODO erro da API (negócio, validação, 401/403 da segurança e erro inesperado). */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(Instant timestamp, int status, String erro, String mensagem, String caminho,
                           List<CampoErroResponse> campos) {

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho) {
        return de(status, mensagem, caminho, List.of());
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoErroResponse> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, caminho, campos);
    }
}
