package br.com.fametro.spring.transportadora.veiculo.repository;

import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Listagem com filtros via Specification (e não @Query): assim o Spring Data valida o {@code sort}
 * e um campo inexistente vira 400, não 500.
 */
@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long>, JpaSpecificationExecutor<Veiculo> {

    boolean existsByPlaca(String placa);

    boolean existsByRenavam(String renavam);

    List<Veiculo> findAllByOrderByPlacaAsc();

    /** Filtros opcionais: nulo = não filtra. */
    static Specification<Veiculo> filtro(StatusVeiculo status, TipoVeiculo tipo) {
        return (root, query, cb) -> {
            var condicoes = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (tipo != null) {
                condicoes.add(cb.equal(root.get("tipo"), tipo));
            }
            return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
