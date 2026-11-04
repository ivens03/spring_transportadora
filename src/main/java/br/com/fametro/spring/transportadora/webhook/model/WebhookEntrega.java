package br.com.fametro.spring.transportadora.webhook.model;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;

/**
 * Um aviso para um webhook (fila de saída + histórico). Nasce PENDENTE na mesma transação da mudança da
 * encomenda; o despachante envia depois e registra o resultado de cada tentativa.
 */
@Entity
@Table(name = "webhook_entrega")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEntrega {

    private static final int TAMANHO_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webhook_id", nullable = false, updatable = false)
    private Webhook webhook;

    /** Nulo no PING. */
    @Column(name = "carga_id", updatable = false)
    private Long cargaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoEventoEncomenda evento;

    /** Corpo JSON enviado (fotografia do momento do evento). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEntregaWebhook status;

    @Column(nullable = false)
    private int tentativas;

    private Instant proximaTentativaEm;
    private Integer ultimoStatusHttp;
    private String ultimoErro;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    private Instant entregueEm;

    public static WebhookEntrega nova(Webhook webhook, Long cargaId, TipoEventoEncomenda evento, String payload, Instant agora) {
        WebhookEntrega e = new WebhookEntrega();
        e.webhook = webhook;
        e.cargaId = cargaId;
        e.evento = evento;
        e.payload = payload;
        e.status = StatusEntregaWebhook.PENDENTE;
        e.proximaTentativaEm = agora;
        e.criadaEm = agora;
        return e;
    }

    public void registrarSucesso(int statusHttp, Instant agora) {
        tentativas++;
        status = StatusEntregaWebhook.ENTREGUE;
        ultimoStatusHttp = statusHttp;
        ultimoErro = null;
        entregueEm = agora;
        proximaTentativaEm = null;
    }

    /** Falhou: agenda a próxima com espera dobrando a cada tentativa, ou desiste ao chegar no limite. */
    public void registrarFalha(Integer statusHttp, String erro, Instant agora, int maxTentativas,
                               Duration esperaInicial, Duration esperaMaxima) {
        tentativas++;
        ultimoStatusHttp = statusHttp;
        ultimoErro = erro == null || erro.length() <= TAMANHO_ERRO ? erro : erro.substring(0, TAMANHO_ERRO);
        if (tentativas >= maxTentativas) {
            status = StatusEntregaWebhook.FALHOU;
            proximaTentativaEm = null;
            return;
        }
        Duration espera = esperaInicial.multipliedBy(1L << Math.min(tentativas - 1, 20));
        proximaTentativaEm = agora.plus(espera.compareTo(esperaMaxima) > 0 ? esperaMaxima : espera);
    }
}
