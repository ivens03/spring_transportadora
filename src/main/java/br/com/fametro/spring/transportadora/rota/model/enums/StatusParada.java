package br.com.fametro.spring.transportadora.rota.model.enums;

public enum StatusParada {
    PENDENTE,
    ENTREGUE,
    /** Destinatário ausente, endereço não encontrado, recusa... (motivo obrigatório). A carga volta para a fila. */
    NAO_ENTREGUE
}
