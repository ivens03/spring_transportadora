package br.com.fametro.spring.transportadora.rota.repository;

import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long>, JpaSpecificationExecutor<Rota> {

    @Query(value = "select nextval('seq_codigo_rota')", nativeQuery = true)
    long proximoNumeroCodigo();

    boolean existsByVeiculoIdAndStatusIn(Long veiculoId, Collection<StatusRota> status);

    boolean existsByEntregadorIdAndStatusIn(Long entregadorId, Collection<StatusRota> status);

    /** Filtros opcionais; o entregador só enxerga as rotas dele. */
    static Specification<Rota> filtro(StatusRota status, String entregadorMatricula) {
        return (root, query, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (entregadorMatricula != null) {
                condicoes.add(cb.equal(root.get("entregador").get("matricula"), entregadorMatricula));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
