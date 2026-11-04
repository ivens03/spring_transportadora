package br.com.fametro.spring.transportadora.rota.repository;

import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosicaoRotaRepository extends JpaRepository<PosicaoRota, Long> {

    Optional<PosicaoRota> findFirstByRotaIdOrderByRegistradaEmDesc(Long rotaId);

    /** Rastro em ordem cronológica; {@code desde} permite ao mapa buscar só o que chegou de novo. */
    List<PosicaoRota> findByRotaIdAndRegistradaEmAfterOrderByRegistradaEmAsc(Long rotaId, Instant desde);

    long countByRotaId(Long rotaId);
}
