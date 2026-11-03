package br.com.fametro.spring.transportadora.service;

import br.com.fametro.spring.transportadora.config.JwtConfig;
import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.dto.response.TokenResponse;
import br.com.fametro.spring.transportadora.model.Usuario;
import br.com.fametro.spring.transportadora.model.enums.TipoCredencial;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RotaCertaProperties props;
    private final Clock clock;

    public TokenResponse emitir(Usuario usuario) {
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
                .claim(JwtConfig.CLAIM_NOME, usuario.getNome())
                .claim(JwtConfig.CLAIM_PERFIL, usuario.getPerfil().name())
                .claim(JwtConfig.CLAIM_CREDENCIAL, tipo.name())
                .claim(JwtConfig.CLAIM_PERMISSOES, usuario.getPerfil().getPermissoes())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", validade.toSeconds(), expiraEm,
                usuario.getMatricula(), usuario.getPerfil(), tipo);
    }
}
