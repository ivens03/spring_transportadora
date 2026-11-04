package br.com.fametro.spring.transportadora.webhook.repository;

import br.com.fametro.spring.transportadora.webhook.model.Webhook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    List<Webhook> findByClienteIdAndAtivoTrue(Long clienteId);

    Page<Webhook> findByClienteId(Long clienteId, Pageable pageable);

    /** Webhook de outro cliente responde 404. */
    Optional<Webhook> findByIdAndClienteId(Long id, Long clienteId);

    long countByClienteId(Long clienteId);

    boolean existsByClienteIdAndUrl(Long clienteId, String url);
}
