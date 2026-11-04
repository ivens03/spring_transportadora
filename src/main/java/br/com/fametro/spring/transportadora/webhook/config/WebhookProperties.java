package br.com.fametro.spring.transportadora.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configurações {@code rotacerta.webhook.*}. Os padrões são os de produção (seguros); o dev afrouxa
 * https e rede interna para testar com um receptor local.
 *
 * @param exigirHttps          URL do webhook precisa ser https
 * @param permitirRedeInterna  aceita URL que resolve para IP interno (localhost, 10.x, 192.168.x...). Em prod
 *                             fica false: sem isso o webhook vira porta para a rede interna (SSRF)
 * @param maxPorCliente        webhooks por cliente
 * @param intervaloLocalizacao no máximo um aviso de LOCALIZACAO por encomenda neste intervalo
 * @param maxTentativas        tentativas de envio antes de desistir (FALHOU)
 * @param esperaInicial        espera após a 1ª falha; dobra a cada falha (backoff exponencial)
 * @param esperaMaxima         teto da espera entre tentativas
 * @param timeoutConexao       tempo para conectar no servidor do cliente
 * @param timeoutResposta      tempo para o servidor do cliente responder
 * @param loteDespacho         quantos avisos o despachante pega por rodada
 * @param reserva              por quanto tempo um aviso pego fica reservado (se a aplicação cair no meio do
 *                             envio, outro despachante tenta de novo depois disso: entrega "pelo menos uma vez")
 */
@ConfigurationProperties("rotacerta.webhook")
public record WebhookProperties(@DefaultValue("true") boolean exigirHttps,
                                @DefaultValue("false") boolean permitirRedeInterna,
                                @DefaultValue("5") int maxPorCliente,
                                @DefaultValue("60s") Duration intervaloLocalizacao,
                                @DefaultValue("8") int maxTentativas,
                                @DefaultValue("30s") Duration esperaInicial,
                                @DefaultValue("1h") Duration esperaMaxima,
                                @DefaultValue("3s") Duration timeoutConexao,
                                @DefaultValue("5s") Duration timeoutResposta,
                                @DefaultValue("20") int loteDespacho,
                                @DefaultValue("2m") Duration reserva) {
}
