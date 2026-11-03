package br.com.fametro.spring.transportadora.autenticacao.config;

import br.com.fametro.spring.transportadora.exception.handler.SecurityExceptionHandler;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

/**
 * Matriz de acesso (ver docs/fluxo-acesso.mmd):
 * <pre>
 * /api/v1/auth/**                 público (tokens, ativações, credencial) — /api/v1/auth/sessao exige token
 * /api/v1/app/**                  credencial APP_ENTREGADOR + perfil ENTREGADOR
 * /api/v1/painel/gestao/**        credencial PAINEL_WEB + ADMINISTRATIVO
 * /api/v1/painel/operacao/**      credencial PAINEL_WEB + SUPORTE
 * /api/v1/painel/financeiro/**    credencial PAINEL_WEB + FINANCEIRO
 * /api/v1/painel/monitoramento/** credencial PAINEL_WEB + DESENVOLVIMENTO
 * /swagger-ui.html, /v3/api-docs  público (documentação)
 * </pre>
 * Os caminhos são relativos ao context path: /transportadora_dev (dev) ou /transportadora (prod).
 * O token do app nunca entra no painel (entregador "sem acesso a painel web") e vice-versa.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                                   SecurityExceptionHandler securityExceptionHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless com Bearer token, sem cookie de sessão
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // públicos: login, ativação e troca de PIN/senha (quem está com a senha vencida não tem token)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/app/tokens", "/api/v1/auth/painel/tokens", "/api/v1/auth/ativacoes").permitAll()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/auth/app/credencial", "/api/v1/auth/painel/credencial").permitAll()
                        // documentação (no prod pode ser desligada com SWAGGER_ENABLED=false)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // módulos
                        .requestMatchers("/api/v1/app/**").access(exige(TipoCredencial.APP_ENTREGADOR, Perfil.ENTREGADOR))
                        .requestMatchers("/api/v1/painel/gestao/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.ADMINISTRATIVO))
                        .requestMatchers("/api/v1/painel/operacao/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.SUPORTE))
                        .requestMatchers("/api/v1/painel/financeiro/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.FINANCEIRO))
                        .requestMatchers("/api/v1/painel/monitoramento/**").access(exige(TipoCredencial.PAINEL_WEB, Perfil.DESENVOLVIMENTO))
                        .requestMatchers("/api/v1/painel/**").hasAuthority(TipoCredencial.PAINEL_WEB.authority())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler));
        return http.build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> exige(TipoCredencial credencial, Perfil perfil) {
        return AuthorizationManagers.allOf(hasAuthority(credencial.authority()), hasRole(perfil.name()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt
    }
}
