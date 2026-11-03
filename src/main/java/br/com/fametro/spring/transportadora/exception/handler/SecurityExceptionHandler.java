package br.com.fametro.spring.transportadora.exception.handler;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * 401 e 403 acontecem no filtro do Spring Security, ANTES do controller — o {@link GlobalExceptionHandler}
 * não os vê. Esta classe responde esses casos com o mesmo {@link ErroResponse}.
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAGEM_401 = "Autenticação necessária: envie um token Bearer válido.";
    static final String MENSAGEM_403 = "Seu perfil ou tipo de credencial não tem acesso a este recurso.";

    /** Mantém o cabeçalho WWW-Authenticate padrão do OAuth2 (RFC 6750). */
    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        bearerEntryPoint.commence(request, response, ex);
        escrever(response, ErroResponse.de(HttpStatus.UNAUTHORIZED, MENSAGEM_401, request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        bearerAccessDenied.handle(request, response, ex);
        escrever(response, ErroResponse.de(HttpStatus.FORBIDDEN, MENSAGEM_403, request.getRequestURI()));
    }

    private void escrever(HttpServletResponse response, ErroResponse corpo) throws IOException {
        response.setStatus(corpo.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), corpo);
    }
}
