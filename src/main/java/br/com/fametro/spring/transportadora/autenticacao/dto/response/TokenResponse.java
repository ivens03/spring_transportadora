package br.com.fametro.spring.transportadora.autenticacao.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * {@code expiraEm}: quando o token vence (12h app / 8h painel).
 * {@code credencialExpiraEm}: quando a senha/PIN vence (chave de salt de 30 dias) — o front pode avisar antes.
 */
@Schema(description = "Token de acesso. Envie no header: Authorization: Bearer <accessToken>")
public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiraEm,
                            String matricula, Perfil perfil, TipoCredencial tipoCredencial,
                            Instant credencialExpiraEm) {
}
