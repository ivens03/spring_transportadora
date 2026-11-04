package br.com.fametro.spring.transportadora.encomenda.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * O que aconteceu com a encomenda. É o nome do evento no webhook (header X-RotaCerta-Evento e campo "evento").
 * {@code historico} = entra na linha do tempo do portal; {@code assinavel} = o cliente escolhe se quer receber.
 */
@Getter
@RequiredArgsConstructor
public enum TipoEventoEncomenda {

    CADASTRADA("Encomenda recebida pela transportadora", true, true),
    ROTEIRIZADA("Encomenda separada para uma rota de entrega", true, true),
    SAIU_PARA_ENTREGA("Saiu para entrega", true, true),
    /** Posição do veículo enquanto a encomenda está a caminho (no máximo um aviso a cada N segundos). */
    LOCALIZACAO("Localização do veículo atualizada", false, true),
    ENTREGUE("Entregue", true, true),
    NAO_ENTREGUE("Tentativa de entrega sem sucesso", true, true),
    /** Voltou com o veículo (não entregue) ou a rota foi cancelada antes de sair: espera outra rota. */
    AGUARDANDO_NOVA_ROTA("Aguardando nova rota de entrega", true, true),
    CANCELADA("Encomenda cancelada", true, true),
    /** Teste disparado pelo cliente (POST /webhooks/{id}/pings): sempre enviado, não precisa assinar. */
    PING("Teste do webhook", false, false);

    private final String descricao;
    private final boolean historico;
    private final boolean assinavel;
}
