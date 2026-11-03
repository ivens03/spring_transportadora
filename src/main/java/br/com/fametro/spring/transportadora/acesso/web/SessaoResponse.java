package br.com.fametro.spring.transportadora.acesso.web;

import java.time.Instant;
import java.util.List;

/** Quem sou eu com este token (o front usa para montar o menu do módulo). */
public record SessaoResponse(String matricula, String nome, String perfil, String tipoCredencial, String modulo,
                             List<String> permissoes, Instant expiraEm) {
}
