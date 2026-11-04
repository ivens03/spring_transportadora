package br.com.fametro.spring.transportadora.usuario.model.enums;

/** Vínculo da matrícula com a transportadora (primeira decisão do fluxo de acesso). */
public enum Vinculo {
    /** Operação externa: entregador, usa somente o app. */
    EXTERNO,
    /** Operação interna: funcionário, usa somente o painel web. */
    INTERNO,
    /** Cliente da transportadora (empresa que despacha encomendas): usa somente o Portal do Cliente. */
    CLIENTE
}
