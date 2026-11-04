package br.com.fametro.spring.transportadora.rota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Leitura de GPS do app durante a rota (somente leitura aqui; a gravação é em lote via JDBC). */
@Entity
@Table(name = "posicao_rota")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PosicaoRota {

    @Id
    private Long id;

    @Column(name = "rota_id", nullable = false)
    private Long rotaId;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    /** Hora do celular no momento da leitura. */
    @Column(nullable = false)
    private Instant registradaEm;

    @Column(name = "velocidade_km_h")
    private BigDecimal velocidadeKmH;

    @Column(name = "precisao_m")
    private BigDecimal precisaoM;

    /** Hora em que o servidor recebeu (pode ser bem depois, se o app estava sem sinal). */
    @Column(nullable = false)
    private Instant recebidaEm;
}
