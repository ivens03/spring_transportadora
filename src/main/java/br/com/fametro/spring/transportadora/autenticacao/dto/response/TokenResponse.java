package br.com.fametro.spring.transportadora.autenticacao.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiraEm,
                            String matricula, Perfil perfil, TipoCredencial tipoCredencial) {
}
