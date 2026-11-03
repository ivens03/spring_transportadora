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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                                   SecurityExceptionHandler securityExceptionHandler) throws Exception {
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
