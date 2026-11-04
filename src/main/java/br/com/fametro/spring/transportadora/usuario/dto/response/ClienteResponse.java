package br.com.fametro.spring.transportadora.usuario.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

/** Visão mínima do cliente para o Suporte vincular a carga (sem dados de acesso). */
public record ClienteResponse(
        @Schema(example = "CLI-000001") String matricula,
        @Schema(example = "Loja Exemplo Ltda") String nome,
        @Schema(example = "logistica@loja.com.br") String email,
        @Schema(example = "ATIVO") StatusUsuario status) {

    public static ClienteResponse de(Usuario u) {
        return new ClienteResponse(u.getMatricula(), u.getNome(), u.getEmail(), u.getStatus());
    }
}
