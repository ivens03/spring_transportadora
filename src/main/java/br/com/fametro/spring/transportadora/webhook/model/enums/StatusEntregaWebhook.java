package br.com.fametro.spring.transportadora.webhook.model.enums;

/** Situação de um aviso na fila de webhooks. */
public enum StatusEntregaWebhook {
    /** Na fila (ainda não enviado ou esperando a próxima tentativa). */
    PENDENTE,
    /** O servidor do cliente respondeu 2xx. */
    ENTREGUE,
    /** Esgotou as tentativas. */
    FALHOU,
    /** Aviso de localização substituído por um mais novo antes de sair (não adianta mandar posição velha). */
    DESCARTADA
}
