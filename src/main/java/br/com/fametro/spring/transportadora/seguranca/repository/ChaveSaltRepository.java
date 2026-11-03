package br.com.fametro.spring.transportadora.seguranca.repository;

import br.com.fametro.spring.transportadora.seguranca.model.ChaveSalt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChaveSaltRepository extends JpaRepository<ChaveSalt, Long> {

    Optional<ChaveSalt> findByUsuarioIdAndAtivaTrue(Long usuarioId);
}
