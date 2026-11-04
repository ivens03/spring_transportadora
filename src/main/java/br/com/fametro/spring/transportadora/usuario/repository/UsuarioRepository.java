package br.com.fametro.spring.transportadora.usuario.repository;

import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByMatricula(String matricula);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    Page<Usuario> findByPerfil(Perfil perfil, Pageable pageable);

    Page<Usuario> findByPerfilAndStatus(Perfil perfil, StatusUsuario status, Pageable pageable);

    @Query(value = "select nextval('seq_matricula_entregador')", nativeQuery = true)
    long proximoNumeroMatriculaEntregador();

    @Query(value = "select nextval('seq_matricula_funcionario')", nativeQuery = true)
    long proximoNumeroMatriculaFuncionario();
}
