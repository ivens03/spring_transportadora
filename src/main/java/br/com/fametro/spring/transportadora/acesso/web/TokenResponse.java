package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiraEm,
                            String matricula, Perfil perfil, TipoCredencial tipoCredencial) {
}
