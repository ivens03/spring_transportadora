package br.com.fametro.spring.transportadora.acesso.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByMatricula(String matricula);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    List<Usuario> findAllByOrderByMatriculaAsc();

    @Query(value = "select nextval('seq_matricula_entregador')", nativeQuery = true)
    long proximoNumeroMatriculaEntregador();

    @Query(value = "select nextval('seq_matricula_funcionario')", nativeQuery = true)
    long proximoNumeroMatriculaFuncionario();
}
