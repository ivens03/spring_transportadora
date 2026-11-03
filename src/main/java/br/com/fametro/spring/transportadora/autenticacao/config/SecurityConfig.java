package br.com.fametro.spring.transportadora.autenticacao.config;

import br.com.fametro.spring.transportadora.exception.handler.SecurityExceptionHandler;
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

import static br.com.fametro.spring.transportadora.usuario.model.Permissoes.ABASTECIMENTO_REGISTRAR;
import static br.com.fametro.spring.transportadora.usuario.model.Permissoes.CONSUMO_VER_METRICAS;
import static br.com.fametro.spring.transportadora.usuario.model.Permissoes.USUARIO_GERENCIAR;
import static br.com.fametro.spring.transportadora.usuario.model.Permissoes.VEICULO_GERENCIAR;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;

/**
 * Matriz de acesso por RECURSO + PERMISSÃO do token (ver docs/fluxo-acesso.mmd). Negado por padrão:
 * só funciona o que estiver liberado aqui.
 * <pre>
 * POST  /api/v1/auth/app/tokens, /painel/tokens, /credenciais   público
 * PATCH /api/v1/auth/credenciais/{matricula}                     público (exige a credencial atual)
 * GET   /api/v1/auth/sessao                                       qualquer token válido
 * /api/v1/usuarios/**                                             painel + usuario:gerenciar       (ADMINISTRATIVO)
 * /api/v1/veiculos/{id}/metricas-consumo, /api/v1/metricas-consumo painel + consumo:ver-metricas  (FINANCEIRO)
 * /api/v1/veiculos/{id}/abastecimentos/**                         painel + abastecimento:registrar (ADM, SUPORTE, FINANCEIRO)
 * /api/v1/veiculos/**                                             painel + veiculo:gerenciar      (ADM, SUPORTE, FINANCEIRO)
 * /swagger-ui.html, /v3/api-docs                                  público (documentação)
 * qualquer outra rota                                             negada
 * </pre>
 * Todos os recursos do painel exigem a credencial PAINEL_WEB: o token do app nunca entra (entregador
 * "sem acesso a painel web"). Caminhos relativos ao context path (/transportadora_dev ou /transportadora).
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
                                "/api/v1/auth/app/tokens", "/api/v1/auth/painel/tokens", "/api/v1/auth/credenciais").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/auth/credenciais/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/sessao").authenticated()
                        // documentação (no prod pode ser desligada com SWAGGER_ENABLED=false)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // recursos do painel (a ordem importa: o mais específico primeiro)
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").access(painel(USUARIO_GERENCIAR))
                        .requestMatchers(HttpMethod.GET, "/api/v1/metricas-consumo", "/api/v1/veiculos/*/metricas-consumo")
                                .access(painel(CONSUMO_VER_METRICAS))
                        .requestMatchers("/api/v1/veiculos/*/abastecimentos", "/api/v1/veiculos/*/abastecimentos/**")
                                .access(painel(ABASTECIMENTO_REGISTRAR))
                        .requestMatchers("/api/v1/veiculos", "/api/v1/veiculos/**").access(painel(VEICULO_GERENCIAR))
                        // negado por padrão
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler));
        return http.build();
    }

    /** Token do painel web E a permissão pedida. */
    private static AuthorizationManager<RequestAuthorizationContext> painel(String permissao) {
        return AuthorizationManagers.allOf(hasAuthority(TipoCredencial.PAINEL_WEB.authority()), hasAuthority(permissao));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt (código de ativação)
    }
}
