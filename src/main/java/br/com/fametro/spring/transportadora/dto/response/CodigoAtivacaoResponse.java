package br.com.fametro.spring.transportadora.dto.response;

import br.com.fametro.spring.transportadora.model.Usuario;
import br.com.fametro.spring.transportadora.model.enums.Departamento;
import br.com.fametro.spring.transportadora.model.enums.Perfil;
import br.com.fametro.spring.transportadora.model.enums.TipoCredencial;
import br.com.fametro.spring.transportadora.model.enums.Vinculo;

import java.time.Instant;

/**
 * Devolvido UMA vez ao administrativo (ao criar o usuário ou reemitir o código), que repassa o código à pessoa.
 * Num sistema real iria por SMS/e-mail. No banco fica apenas o hash.
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
