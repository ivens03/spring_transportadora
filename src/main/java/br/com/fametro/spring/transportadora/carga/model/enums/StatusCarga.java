package br.com.fametro.spring.transportadora.carga.model.enums;

/** Ciclo da carga: AGUARDANDO_ROTA → ROTEIRIZADA → EM_TRANSITO → ENTREGUE (ou volta a AGUARDANDO_ROTA). */
public enum StatusCarga {
    /** Disponível para o Suporte colocar numa rota. */
    AGUARDANDO_ROTA,
    /** Está numa rota PLANEJADA (ainda no depósito). */
    ROTEIRIZADA,
    /** A rota foi iniciada: está no veículo. */
    EM_TRANSITO,
    ENTREGUE,
    CANCELADA
}
