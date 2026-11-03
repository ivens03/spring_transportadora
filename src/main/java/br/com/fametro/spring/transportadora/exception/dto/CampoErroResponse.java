package br.com.fametro.spring.transportadora.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Erro de validação de um campo do request. */
@Schema(description = "Erro de validação de um campo")
public record CampoErroResponse(
        @Schema(example = "nome") String campo,
        @Schema(example = "O nome é obrigatório.") String mensagem) {
}
