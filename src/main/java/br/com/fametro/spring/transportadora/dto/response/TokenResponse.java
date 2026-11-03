package br.com.fametro.spring.transportadora.dto.response;

import br.com.fametro.spring.transportadora.model.enums.Perfil;
import br.com.fametro.spring.transportadora.model.enums.TipoCredencial;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiraEm,
                            String matricula, Perfil perfil, TipoCredencial tipoCredencial) {
}
