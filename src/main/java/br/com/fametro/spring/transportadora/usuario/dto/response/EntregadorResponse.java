package br.com.fametro.spring.transportadora.usuario.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

/** Visão mínima do entregador para quem monta rota (sem e-mail, sem dados de acesso). */
public record EntregadorResponse(
        @Schema(example = "ENT-000001") String matricula,
        @Schema(example = "João Lima") String nome,
        @Schema(example = "ATIVO") StatusUsuario status) {

    public static EntregadorResponse de(Usuario u) {
        return new EntregadorResponse(u.getMatricula(), u.getNome(), u.getStatus());
    }
}
