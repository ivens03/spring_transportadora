package br.com.fametro.spring.transportadora.rota.model;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
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

import java.math.BigDecimal;
import java.time.Instant;

/** Uma parada da rota = uma carga a entregar, na ordem definida pelo otimizador. */
@Entity
@Table(name = "parada_rota")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParadaRota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rota_id", nullable = false, updatable = false)
    private Rota rota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_id", nullable = false, updatable = false)
    private Carga carga;

    /** 1, 2, 3... */
    @Column(nullable = false, updatable = false)
    private Integer ordem;

    @Column(nullable = false, updatable = false)
    private BigDecimal distanciaDoAnteriorKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParada status;

    private String motivo;

    private Instant finalizadaEm;

    static ParadaRota nova(Rota rota, Carga carga, int ordem, BigDecimal distanciaDoAnteriorKm) {
        ParadaRota p = new ParadaRota();
        p.rota = rota;
        p.carga = carga;
        p.ordem = ordem;
        p.distanciaDoAnteriorKm = distanciaDoAnteriorKm;
        p.status = StatusParada.PENDENTE;
        return p;
    }

    public boolean pendente() {
        return status == StatusParada.PENDENTE;
    }

    void baixar(StatusParada resultado, String motivo, Instant quando) {
        this.status = resultado;
        this.motivo = motivo;
        this.finalizadaEm = quando;
    }
}
