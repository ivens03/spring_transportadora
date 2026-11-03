package br.com.fametro.spring.transportadora.autenticacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Primeiro acesso: troca o código de ativação pelo PIN (entregador) ou senha (funcionário)")
public record AtivacaoRequest(
        @Schema(description = "Matrícula recebida no cadastro", example = "ENT-000001")
        @NotBlank(message = "A matrícula é obrigatória.") String matricula,
        @Schema(description = "Código de 8 caracteres (uso único, vale 72h)", example = "R26CWD5F")
        @NotBlank(message = "O código de ativação é obrigatório.") String codigoAtivacao,
        @Schema(description = "Entregador: PIN de 6 dígitos (sem repetidos/sequência). Funcionário: senha ≥ 10 caracteres com letras e números.",
                example = "482917")
        @NotBlank(message = "Informe o PIN (app) ou a senha (painel).") String novaCredencial) {
}
