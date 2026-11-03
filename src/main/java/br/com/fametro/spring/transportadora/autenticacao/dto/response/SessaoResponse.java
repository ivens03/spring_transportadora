package br.com.fametro.spring.transportadora.autenticacao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Quem sou eu com este token (o front usa para montar o menu do módulo). */
@Schema(description = "Dados do dono do token (perfil, módulo e permissões)")
public record SessaoResponse(String matricula, String nome, String perfil, String tipoCredencial, String modulo,
                             List<String> permissoes, Instant expiraEm) {
}
