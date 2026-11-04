package br.com.fametro.spring.transportadora.rota.repository;

import br.com.fametro.spring.transportadora.rota.dto.request.PosicaoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

/**
 * Gravação do lote de posições via JDBC: {@code on conflict do nothing} descarta reenvios do app
 * (mesma rota + mesmo instante) sem derrubar o lote inteiro.
 */
@Repository
@RequiredArgsConstructor
public class PosicaoRotaLoteRepository {

    private static final String SQL = """
            insert into posicao_rota (rota_id, latitude, longitude, registrada_em, velocidade_km_h, precisao_m, recebida_em)
            values (?, ?, ?, ?, ?, ?, ?)
            on conflict (rota_id, registrada_em) do nothing""";

    private final JdbcTemplate jdbc;

    /** @return quantas posições eram novas (as demais já existiam) */
    public int inserirNovas(Long rotaId, List<PosicaoRequest> posicoes, Instant agora) {
        int[][] resultado = jdbc.batchUpdate(SQL, posicoes, 200, (ps, p) -> {
            ps.setLong(1, rotaId);
            ps.setBigDecimal(2, p.latitude());
            ps.setBigDecimal(3, p.longitude());
            ps.setTimestamp(4, Timestamp.from(p.registradaEm().toInstant()));
            ps.setObject(5, p.velocidadeKmH(), Types.NUMERIC);
            ps.setObject(6, p.precisaoM(), Types.NUMERIC);
            ps.setTimestamp(7, Timestamp.from(agora));
        });
        int novas = 0;
        for (int[] bloco : resultado) {
            for (int linhas : bloco) {
                novas += Math.max(linhas, 0);
            }
        }
        return novas;
    }
}
