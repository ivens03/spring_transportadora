package br.com.fametro.spring.transportadora.rota.repository;

import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParadaRotaRepository extends JpaRepository<ParadaRota, Long> {

    /** Paradas da carga, da rota mais recente para a mais antiga (já com rota, veículo e entregador). */
    @Query("""
            select p from ParadaRota p
              join fetch p.rota r join fetch r.veiculo join fetch r.entregador
             where p.carga.id = :cargaId
             order by r.criadaEm desc, p.id desc""")
    List<ParadaRota> maisRecentesDaCarga(@Param("cargaId") Long cargaId, Limit limit);
}
