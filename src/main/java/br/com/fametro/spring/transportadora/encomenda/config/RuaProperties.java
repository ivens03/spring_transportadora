package br.com.fametro.spring.transportadora.encomenda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configurações {@code rotacerta.rua.*}: descobrir o nome da rua pela posição do GPS (geocodificação reversa).
 * Usado no rastreio da encomenda e no webhook de LOCALIZACAO ("Na Av. Bezerra de Menezes · faltam ~12 min").
 *
 * @param ativo           false = nunca consulta (testes); o rastreio sai sem a rua
 * @param url             endpoint {@code /reverse} do Nominatim (OpenStreetMap, gratuito, sem chave)
 * @param agente          User-Agent: a política do Nominatim exige identificar a aplicação
 * @param timeout         tempo máximo da consulta (roda dentro da transação que recebe o GPS: precisa ser curto)
 * @param intervaloMinimo entre duas consultas ao serviço (política do Nominatim: no máximo 1 por segundo)
 * @param pausaAposFalha  se o serviço não responde, para de tentar por este tempo (não atrasa cada envio de GPS)
 * @param maxCache        posições guardadas na memória (arredondadas a ~11 m)
 */
@ConfigurationProperties("rotacerta.rua")
public record RuaProperties(@DefaultValue("true") boolean ativo,
                            @DefaultValue("https://nominatim.openstreetmap.org/reverse") String url,
                            @DefaultValue("RotaCerta/1.0 (MBA Fametro - transportadora)") String agente,
                            @DefaultValue("2s") Duration timeout,
                            @DefaultValue("1s") Duration intervaloMinimo,
                            @DefaultValue("1m") Duration pausaAposFalha,
                            @DefaultValue("5000") int maxCache) {
}
