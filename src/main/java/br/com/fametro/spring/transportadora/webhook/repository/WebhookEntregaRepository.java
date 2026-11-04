package br.com.fametro.spring.transportadora.webhook.repository;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.webhook.model.WebhookEntrega;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface WebhookEntregaRepository extends JpaRepository<WebhookEntrega, Long> {

    Page<WebhookEntrega> findByWebhookId(Long webhookId, Pageable pageable);

    Page<WebhookEntrega> findByWebhookIdAndStatus(Long webhookId, StatusEntregaWebhook status, Pageable pageable);

    /** Aviso já com o webhook carregado (o despachante precisa da URL e do segredo). */
    @Query("select e from WebhookEntrega e join fetch e.webhook where e.id = :id")
    Optional<WebhookEntrega> carregarComWebhook(@Param("id") Long id);

    /** Quando foi criado o último aviso deste evento para a encomenda (controle de frequência da localização). */
    @Query("""
            select max(e.criadaEm) from WebhookEntrega e
             where e.webhook.id = :webhookId and e.cargaId = :cargaId and e.evento = :evento""")
    Instant ultimoCriado(@Param("webhookId") Long webhookId, @Param("cargaId") Long cargaId,
                         @Param("evento") TipoEventoEncomenda evento);

    /** Localização nova torna a pendente antiga inútil: ela é descartada. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update WebhookEntrega e set e.status = :descartada, e.proximaTentativaEm = null
             where e.webhook.id = :webhookId and e.cargaId = :cargaId and e.evento = :evento and e.status = :pendente""")
    int descartarPendentes(@Param("webhookId") Long webhookId, @Param("cargaId") Long cargaId,
                           @Param("evento") TipoEventoEncomenda evento,
                           @Param("pendente") StatusEntregaWebhook pendente,
                           @Param("descartada") StatusEntregaWebhook descartada);
}
