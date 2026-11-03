package br.com.fametro.spring.transportadora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Assinatura/validação do JWT (HS256) e conversão das claims em authorities do Spring Security. */
@Configuration
public class JwtConfig {

    public static final String CLAIM_NOME = "nome";
    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_CREDENCIAL = "cred";
    public static final String CLAIM_PERMISSOES = "permissoes";

    @Bean
    public SecretKey chaveAssinaturaJwt(RotaCertaProperties props) {
        byte[] segredo = props.jwt().segredo().getBytes(StandardCharsets.UTF_8);
        if (segredo.length < 32) {
            throw new IllegalStateException("rotacerta.jwt.segredo precisa ter pelo menos 32 bytes (HS256).");
        }
        return new SecretKeySpec(segredo, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chave) {
        return NimbusJwtEncoder.withSecretKey(chave).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chave, RotaCertaProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().emissor()));
        return decoder;
    }

    /** Claims do token -> authorities: ROLE_&lt;perfil&gt;, CRED_&lt;tipo&gt; e cada permissão do módulo. */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            String perfil = jwt.getClaimAsString(CLAIM_PERFIL);
            if (perfil != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + perfil));
            }
            String credencial = jwt.getClaimAsString(CLAIM_CREDENCIAL);
            if (credencial != null) {
                authorities.add(new SimpleGrantedAuthority("CRED_" + credencial));
            }
            List<String> permissoes = jwt.getClaimAsStringList(CLAIM_PERMISSOES);
            if (permissoes != null) {
                permissoes.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }
}
