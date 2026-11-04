package br.com.fametro.spring.transportadora.carga.repository;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Repository
public interface CargaRepository extends JpaRepository<Carga, Long>, JpaSpecificationExecutor<Carga> {

    @Query(value = "select nextval('seq_codigo_carga')", nativeQuery = true)
    long proximoNumeroCodigo();

    /** Trava as cargas escolhidas: duas rotas montadas ao mesmo tempo não levam a mesma carga. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carga c where c.id in :ids order by c.id")
    List<Carga> travarPorIds(@Param("ids") Collection<Long> ids);

    /** Filtros opcionais (nulo = não filtra); cidade e bairro sem diferenciar maiúscula. */
    static Specification<Carga> filtro(StatusCarga status, String cidade, String bairro) {
        return (root, query, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (cidade != null && !cidade.isBlank()) {
                condicoes.add(cb.equal(cb.lower(root.get("cidade")), cidade.trim().toLowerCase(Locale.ROOT)));
            }
            if (bairro != null && !bairro.isBlank()) {
                condicoes.add(cb.equal(cb.lower(root.get("bairro")), bairro.trim().toLowerCase(Locale.ROOT)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
