package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.autenticacao.config.JwtConfig;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quem está chamando: matrícula + se veio do app do entregador.
 * O mesmo recurso /rotas atende o painel (vê tudo) e o app (só as próprias rotas).
 */
public record Solicitante(String matricula, boolean entregador) {

    public static Solicitante de(Jwt jwt) {
        return new Solicitante(jwt.getSubject(),
                TipoCredencial.APP_ENTREGADOR.name().equals(jwt.getClaimAsString(JwtConfig.CLAIM_CREDENCIAL)));
    }
}
