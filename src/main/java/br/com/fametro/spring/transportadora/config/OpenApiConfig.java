package br.com.fametro.spring.transportadora.config;

import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI. O mesmo código atende os dois perfis; muda o que vem do properties:
 * <ul>
 *   <li>dev : título "DEV", servidor {@code /transportadora_dev}</li>
 *   <li>prod: título "PROD", servidor {@code /transportadora} (pode ser desligado com SWAGGER_ENABLED=false)</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    /** Nome do esquema de segurança usado em {@code @SecurityRequirement(name = BEARER)}. */
    public static final String BEARER = "bearerAuth";

    private static final String SCHEMA_ERRO = "#/components/schemas/ErroResponse";

    @Bean
    public OpenAPI rotaCertaOpenApi(@Value("${rotacerta.openapi.ambiente:local}") String ambiente,
                                    @Value("${server.servlet.context-path:}") String contextPath) {
        return new OpenAPI()
                .info(new Info()
                        .title("RotaCerta API — " + ambiente)
                        .version("v1")
                        .description("""
                                Logística e rastreamento de entregas (MBA Backend 03).

                                **Como autenticar**
                                1. Entregador: `POST /api/v1/auth/app/tokens` (matrícula + PIN).
                                   Funcionário: `POST /api/v1/auth/painel/tokens` (e-mail + senha).
                                2. Copie o `accessToken` e clique em **Authorize**.

                                Erros sempre no formato `ErroResponse`. Senha/PIN vence a cada 30 dias
                                (403 com `codigo = CREDENCIAL_EXPIRADA`).""")
                        .contact(new Contact().name("Equipe RotaCerta")))
                .servers(List.of(new Server()
                        .url(contextPath.isBlank() ? "/" : contextPath)
                        .description(ambiente)))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtido no login (app ou painel).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Respostas de erro comuns a todos os endpoints, sem repetir anotação em cada método:
     * 500 em todos; 401 e 403 nos que exigem token.
     */
    @Bean
    public OpenApiCustomizer respostasDeErroPadrao() {
        return openApi -> {
            @SuppressWarnings("rawtypes")
            java.util.Map<String, Schema> schemas = ModelConverters.getInstance().read(ErroResponse.class);
            schemas.forEach(openApi.getComponents()::addSchemas);

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operacao -> {
                adicionar(operacao, "500", "Erro inesperado (detalhe só no log do servidor).");
                if (exigeToken(operacao)) {
                    adicionar(operacao, "401", "Sem token ou token inválido/expirado.");
                    adicionar(operacao, "403", "Perfil ou tipo de credencial sem acesso a este recurso.");
                }
            }));
        };
    }

    /** Operação sem {@code security} herda o global (exige token); com lista vazia é pública. */
    private static boolean exigeToken(Operation operacao) {
        return operacao.getSecurity() == null || !operacao.getSecurity().isEmpty();
    }

    private static void adicionar(Operation operacao, String status, String descricao) {
        if (operacao.getResponses().containsKey(status)) {
            return;
        }
        operacao.getResponses().addApiResponse(status, new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(SCHEMA_ERRO)))));
    }
}
