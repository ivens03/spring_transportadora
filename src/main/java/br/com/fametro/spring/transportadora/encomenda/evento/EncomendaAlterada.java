package br.com.fametro.spring.transportadora.encomenda.evento;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;

import java.time.Instant;

/**
 * Evento de aplicação (Spring) publicado por carga/rota quando a encomenda muda. Os ouvintes rodam na MESMA
 * transação (síncronos): gravam a linha do tempo e enfileiram os webhooks junto com a mudança (outbox).
 * Carga sem cliente é ignorada pelos ouvintes.
 *
 * @param rotaCodigo rota envolvida (nulo no cadastro/cancelamento)
 * @param detalhe    texto livre, ex.: motivo da não entrega
 */
public record EncomendaAlterada(Carga carga, TipoEventoEncomenda tipo, String rotaCodigo, String detalhe,
                                Instant ocorridoEm) {
}
