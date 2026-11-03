package br.com.fametro.spring.transportadora.autenticacao.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.enums.TipoCredencial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Credencial criada (ativação) ou trocada. Nunca devolve o PIN/senha nem o hash. */
@Schema(description = "Credencial do usuário: tipo e até quando vale (chave de salt de 30 dias)")
public record CredencialResponse(
        @Schema(example = "ENT-000001") String matricula,
        @Schema(example = "APP_ENTREGADOR") TipoCredencial tipoCredencial,
        @Schema(description = "Quando a senha/PIN vence e a troca passa a ser obrigatória") Instant expiraEm) {
}
