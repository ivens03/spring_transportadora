package br.com.fametro.spring.transportadora.model.enums;

/** Vínculo da matrícula com a transportadora (primeira decisão do fluxo de acesso). */
public enum Vinculo {
    /** Operação externa: entregador, usa somente o app. */
    EXTERNO,
    /** Operação interna: funcionário, usa somente o painel web. */
    INTERNO
}
