package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.dominio.Departamento;
import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import br.com.fametro.spring.transportadora.acesso.dominio.StatusUsuario;
import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import br.com.fametro.spring.transportadora.acesso.dominio.Usuario;
import br.com.fametro.spring.transportadora.acesso.dominio.Vinculo;

import java.time.Instant;

public record UsuarioResponse(String matricula, String nome, String email, Vinculo vinculo, Departamento departamento,
                              Perfil perfil, TipoCredencial tipoCredencial, String modulo, StatusUsuario status,
                              Instant ultimoLoginEm, Instant criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        Perfil p = u.getPerfil();
        return new UsuarioResponse(u.getMatricula(), u.getNome(), u.getEmail(), p.getVinculo(), p.departamento(), p,
                p.getTipoCredencial(), p.getModulo(), u.getStatus(), u.getUltimoLoginEm(), u.getCriadoEm());
    }
}
