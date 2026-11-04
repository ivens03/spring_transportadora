package br.com.fametro.spring.transportadora.encomenda.evento;

/**
 * O app do entregador mandou posições novas da rota (publicado pelo rastreamento, na mesma transação).
 * O webhook usa para avisar a localização às encomendas ainda pendentes dessa rota.
 */
public record PosicoesRecebidas(Long rotaId) {
}
