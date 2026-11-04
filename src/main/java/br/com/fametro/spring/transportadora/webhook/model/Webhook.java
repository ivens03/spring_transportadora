package br.com.fametro.spring.transportadora.webhook.model;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/** Endereço do cliente que recebe os avisos das encomendas dele. */
@Entity
@Table(name = "webhook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Usuario cliente;

    @Column(nullable = false)
    private String url;

    /** Chave do HMAC-SHA256 da assinatura. Mostrada ao cliente só na criação/rotação. */
    @Column(nullable = false)
    private String segredo;

    @Convert(converter = EventosConverter.class)
    @Column(nullable = false)
    private Set<TipoEventoEncomenda> eventos;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public static Webhook novo(Usuario cliente, String url, String segredo, Set<TipoEventoEncomenda> eventos, Instant agora) {
        Webhook w = new Webhook();
        w.cliente = cliente;
        w.url = url;
        w.segredo = segredo;
        w.eventos = EnumSet.copyOf(eventos);
        w.ativo = true;
        w.criadoEm = agora;
        w.atualizadoEm = agora;
        return w;
    }

    /** Ativo e assinou o evento (PING sempre vai: é o teste que o próprio cliente pediu). */
    public boolean recebe(TipoEventoEncomenda tipo) {
        return ativo && (!tipo.isAssinavel() || eventos.contains(tipo));
    }

    public void alterarUrl(String v, Instant agora) { url = v; atualizadoEm = agora; }
    public void alterarEventos(Set<TipoEventoEncomenda> v, Instant agora) { eventos = EnumSet.copyOf(v); atualizadoEm = agora; }
    public void alterarAtivo(boolean v, Instant agora) { ativo = v; atualizadoEm = agora; }
    public void trocarSegredo(String v, Instant agora) { segredo = v; atualizadoEm = agora; }
}
