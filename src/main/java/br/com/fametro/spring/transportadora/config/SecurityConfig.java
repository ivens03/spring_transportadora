package br.com.fametro.spring.transportadora.config;

import br.com.fametro.spring.transportadora.acesso.dominio.Perfil;
import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

/**
 * Matriz de acesso (ver docs/fluxo-acesso.mmd):
 * <pre>
 * /api/auth/**                 público (login app, login painel, ativação) — /api/auth/me exige token
 * /api/app/**                  credencial APP_ENTREGADOR + perfil ENTREGADOR
 * /api/painel/gestao/**        credencial PAINEL_WEB + ADMINISTRATIVO
 * /api/painel/operacao/**      credencial PAINEL_WEB + SUPORTE
 * /api/painel/financeiro/**    credencial PAINEL_WEB + FINANCEIRO
 * /api/painel/monitoramento/** credencial PAINEL_WEB + DESENVOLVIMENTO
 * </pre>
 * O token do app nunca entra no painel (entregador "sem acesso a painel web") e vice-versa.
 */
@Configuration
public class SecurityConfig {

    static final String CLAIM_PERFIL = "perfil";
    static final String CLAIM_CREDENCIAL = "cred";
    static final String CLAIM_PERMISSOES = "permissoes";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless com Bearer token, sem cookie de sessão
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/app/login", "/api/auth/painel/login", "/api/auth/ativar").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/app/**").access(exige(TipoCredencial.APP_ENTREGADOR, Perfil.ENTREGADOR))
                        .requestMatchers("/api/painel/gestao/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.ADMINISTRATIVO))
                        .requestMatchers("/api/painel/operacao/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.SUPORTE))
                        .requestMatchers("/api/painel/financeiro/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.FINANCEIRO))
                        .requestMatchers("/api/painel/monitoramento/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.DESENVOLVIMENTO))
                        .requestMatchers("/api/painel/**").hasAuthority(TipoCredencial.PAINEL_WEB.authority())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(converterDeAuthorities())));
        return http.build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> exige(TipoCredencial credencial, Perfil perfil) {
        return AuthorizationManagers.allOf(hasAuthority(credencial.authority()), hasRole(perfil.name()));
    }

    /** Claims do token -> authorities: ROLE_&lt;perfil&gt;, CRED_&lt;tipo&gt; e cada permissão do módulo. */
    static Converter<Jwt, AbstractAuthenticationToken> converterDeAuthorities() {
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

    @Bean
    SecretKey chaveAssinaturaJwt(RotaCertaProperties props) {
        byte[] segredo = props.jwt().segredo().getBytes(StandardCharsets.UTF_8);
        if (segredo.length < 32) {
            throw new IllegalStateException("rotacerta.jwt.segredo precisa ter pelo menos 32 bytes (HS256).");
        }
        return new SecretKeySpec(segredo, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chave) {
        return NimbusJwtEncoder.withSecretKey(chave).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chave, RotaCertaProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().emissor()));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
