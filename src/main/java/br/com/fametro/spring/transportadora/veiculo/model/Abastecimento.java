package br.com.fametro.spring.transportadora.veiculo.model;

import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
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

/** Abastecimento (tanque cheio) de um veículo: base das métricas de consumo. */
@Entity
@Table(name = "abastecimento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Abastecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false, updatable = false)
    private Veiculo veiculo;

    /** Quando abasteceu (pode chegar atrasado: o app/painel envia depois). */
    @Column(nullable = false, updatable = false)
    private Instant dataHora;

    @Column(nullable = false, updatable = false)
    private Integer odometroKm;

    @Column(nullable = false, updatable = false)
    private BigDecimal litros;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Combustivel combustivel;

    @Column(updatable = false)
    private String posto;

    @Column(nullable = false, updatable = false)
    private String registradoPor;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    public static Abastecimento novo(Veiculo veiculo, Instant dataHora, Integer odometroKm, BigDecimal litros,
                                     BigDecimal valorTotal, Combustivel combustivel, String posto,
                                     String registradoPor, Instant agora) {
        Abastecimento a = new Abastecimento();
        a.veiculo = veiculo;
        a.dataHora = dataHora;
        a.odometroKm = odometroKm;
        a.litros = litros;
        a.valorTotal = valorTotal;
        a.combustivel = combustivel;
        a.posto = posto;
        a.registradoPor = registradoPor;
        a.criadoEm = agora;
        return a;
    }
}
