package br.com.fametro.spring.transportadora.usuario.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import br.com.fametro.spring.transportadora.usuario.model.enums.Vinculo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Devolvido UMA vez ao administrativo (ao criar o usuário ou reemitir o código), que repassa o código à pessoa.
 * Num sistema real iria por SMS/e-mail. No banco fica apenas o hash.
 */
@Schema(description = "Usuário + código de ativação (mostrado uma única vez)")
public record CodigoAtivacaoResponse(String matricula, String nome, Vinculo vinculo, Departamento departamento,
                                     Perfil perfil, TipoCredencial tipoCredencial, String modulo,
                                     String codigoAtivacao, Instant codigoExpiraEm, String instrucao) {

    public static CodigoAtivacaoResponse de(Usuario u, String codigo, Instant expiraEm) {
        Perfil p = u.getPerfil();
        String instrucao = p.getTipoCredencial() == TipoCredencial.APP_ENTREGADOR
                ? "No app, informe matrícula + código e cadastre um PIN de 6 dígitos (POST /api/v1/auth/credenciais)."
                : "No painel, informe matrícula + código e cadastre uma senha (POST /api/v1/auth/credenciais). O login é pelo e-mail.";
        return new CodigoAtivacaoResponse(u.getMatricula(), u.getNome(), p.getVinculo(), p.departamento(), p,
                p.getTipoCredencial(), p.getModulo(), codigo, expiraEm, instrucao);
    }
}
