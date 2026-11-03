package br.com.fametro.spring.transportadora.veiculo.repository;

import br.com.fametro.spring.transportadora.veiculo.model.Abastecimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbastecimentoRepository extends JpaRepository<Abastecimento, Long> {

    Page<Abastecimento> findByVeiculoId(Long veiculoId, Pageable pageable);

    Optional<Abastecimento> findByIdAndVeiculoId(Long id, Long veiculoId);

    boolean existsByVeiculoIdAndDataHora(Long veiculoId, Instant dataHora);

    /** Vizinho anterior (por data) — para validar odômetro de abastecimento que chega fora de ordem. */
    Optional<Abastecimento> findFirstByVeiculoIdAndDataHoraLessThanOrderByDataHoraDesc(Long veiculoId, Instant dataHora);

    /** Vizinho seguinte (por data). */
    Optional<Abastecimento> findFirstByVeiculoIdAndDataHoraGreaterThanOrderByDataHoraAsc(Long veiculoId, Instant dataHora);

    @Query("""
            select a from Abastecimento a
             where a.veiculo.id = :veiculoId and a.dataHora >= :inicio and a.dataHora < :fim
             order by a.dataHora""")
    List<Abastecimento> doVeiculoNoPeriodo(@Param("veiculoId") Long veiculoId,
                                           @Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("""
            select a from Abastecimento a
             where a.dataHora >= :inicio and a.dataHora < :fim
             order by a.veiculo.id, a.dataHora""")
    List<Abastecimento> daFrotaNoPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
