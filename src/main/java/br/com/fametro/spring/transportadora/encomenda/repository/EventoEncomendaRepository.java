package br.com.fametro.spring.transportadora.encomenda.repository;

import br.com.fametro.spring.transportadora.encomenda.model.EventoEncomenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoEncomendaRepository extends JpaRepository<EventoEncomenda, Long> {

    List<EventoEncomenda> findByCargaIdOrderByOcorridoEmAscIdAsc(Long cargaId);
}
