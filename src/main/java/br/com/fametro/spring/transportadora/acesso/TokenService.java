package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import br.com.fametro.spring.transportadora.acesso.dominio.Usuario;
import br.com.fametro.spring.transportadora.acesso.web.TokenResponse;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Emite o JWT. A validade depende do tipo de credencial (app = jornada inteira; painel = expediente). */
@Service
class TokenService {

    private final JwtEncoder encoder;
    private final RotaCertaProperties props;
    private final Clock clock;

    TokenService(JwtEncoder encoder, RotaCertaProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    TokenResponse emitir(Usuario usuario) {
        TipoCredencial tipo = usuario.tipoCredencial();
        Duration validade = tipo == TipoCredencial.APP_ENTREGADOR
                ? props.credencial().validadeTokenApp()
                : props.credencial().validadeTokenPainel();
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(validade);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().emissor())
                .subject(usuario.getMatricula())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().name())
                .claim("cred", tipo.name())
                .claim("permissoes", usuario.getPerfil().getPermissoes())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", validade.toSeconds(), expiraEm,
                usuario.getMatricula(), usuario.getPerfil(), tipo);
    }
}
