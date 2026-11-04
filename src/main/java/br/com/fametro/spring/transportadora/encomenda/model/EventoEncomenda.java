package br.com.fametro.spring.transportadora.encomenda.model;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Marco da linha do tempo da encomenda (o que o cliente vê no portal). */
@Entity
@Table(name = "encomenda_evento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carga_id", nullable = false, updatable = false)
    private Long cargaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoEventoEncomenda tipo;

    @Column(updatable = false)
    private String rotaCodigo;

    @Column(updatable = false)
    private String detalhe;

    @Column(nullable = false, updatable = false)
    private Instant ocorridoEm;

    public static EventoEncomenda novo(Long cargaId, TipoEventoEncomenda tipo, String rotaCodigo, String detalhe,
                                       Instant ocorridoEm) {
        EventoEncomenda e = new EventoEncomenda();
        e.cargaId = cargaId;
        e.tipo = tipo;
        e.rotaCodigo = rotaCodigo;
        e.detalhe = detalhe;
        e.ocorridoEm = ocorridoEm;
        return e;
    }
}
