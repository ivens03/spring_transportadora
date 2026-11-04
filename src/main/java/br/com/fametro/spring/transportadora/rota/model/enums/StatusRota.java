package br.com.fametro.spring.transportadora.rota.model.enums;

public enum StatusRota {
    /** Gerada pelo Suporte; cargas ainda no depósito. */
    PLANEJADA,
    /** O entregador iniciou no app; posições de GPS chegando. */
    EM_ANDAMENTO,
    /** Todas as paradas baixadas (entregue ou não entregue). Automático. */
    CONCLUIDA,
    /** Cancelada pelo Suporte antes de sair. */
    CANCELADA;

    public boolean ativa() {
        return this == PLANEJADA || this == EM_ANDAMENTO;
    }
}
