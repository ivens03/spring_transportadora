package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.dominio.Departamento;
import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import br.com.fametro.spring.transportadora.acesso.dominio.Usuario;
import br.com.fametro.spring.transportadora.acesso.dominio.Vinculo;

import java.time.Instant;

/**
 * Devolvido UMA vez ao administrativo, que repassa o código à pessoa (num sistema real iria por SMS/e-mail).
 * No banco fica apenas o hash.
 */
public record CodigoAtivacaoResponse(String matricula, String nome, Vinculo vinculo, Departamento departamento,
                                     Perfil perfil, TipoCredencial tipoCredencial, String modulo,
                                     String codigoAtivacao, Instant codigoExpiraEm, String instrucao) {

    public static CodigoAtivacaoResponse de(Usuario u, String codigo, Instant expiraEm) {
        Perfil p = u.getPerfil();
        String instrucao = p.getTipoCredencial() == TipoCredencial.APP_ENTREGADOR
                ? "No app, informe matrícula + código e cadastre um PIN de 6 dígitos (POST /api/auth/ativar)."
                : "No painel, informe matrícula + código e cadastre uma senha (POST /api/auth/ativar). O login é pelo e-mail.";
        return new CodigoAtivacaoResponse(u.getMatricula(), u.getNome(), p.getVinculo(), p.departamento(), p,
                p.getTipoCredencial(), p.getModulo(), codigo, expiraEm, instrucao);
    }
}
