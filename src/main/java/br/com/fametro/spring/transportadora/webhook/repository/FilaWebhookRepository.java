package br.com.fametro.spring.transportadora.webhook.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Reserva de avisos da fila (SQL puro: precisa de FOR UPDATE SKIP LOCKED + UPDATE ... RETURNING).
 * Vários despachantes (várias instâncias da API) podem rodar juntos sem pegar o mesmo aviso.
 */
@Repository
@RequiredArgsConstructor
public class FilaWebhookRepository {

    private static final String RESERVAR = """
            update webhook_entrega set proxima_tentativa_em = ?
             where id in (select id from webhook_entrega
                           where status = 'PENDENTE' and proxima_tentativa_em <= ?
                           order by proxima_tentativa_em, id
                           limit ?
                           for update skip locked)
            returning id""";

    private final JdbcTemplate jdbc;

    /** Pega até {@code lote} avisos vencidos e empurra a próxima tentativa para {@code reservadoAte}. Exige transação. */
    public List<Long> reservar(Instant agora, Instant reservadoAte, int lote) {
        return jdbc.queryForList(RESERVAR, Long.class, Timestamp.from(reservadoAte), Timestamp.from(agora), lote);
    }
}
