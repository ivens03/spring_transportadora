package br.com.fametro.spring.transportadora.dto.response;

import br.com.fametro.spring.transportadora.model.Usuario;
import br.com.fametro.spring.transportadora.model.enums.Departamento;
import br.com.fametro.spring.transportadora.model.enums.Perfil;
import br.com.fametro.spring.transportadora.model.enums.StatusUsuario;
import br.com.fametro.spring.transportadora.model.enums.TipoCredencial;
import br.com.fametro.spring.transportadora.model.enums.Vinculo;

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
