package br.com.fametro.spring.transportadora.rota.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configurações {@code rotacerta.rota.*} da roteirização.
 *
 * @param origem               depósito/CD de onde as rotas saem quando o pedido não informa outra origem
 * @param fatorDistanciaViaria linha reta × fator ≈ distância pelas ruas (estimativa sem serviço de mapas)
 * @param velocidadeMediaKmH   velocidade média urbana para estimar a duração
 * @param minutosPorParada     tempo médio de cada entrega (estacionar, entregar, assinar)
 * @param maxParadas           limite de cargas numa rota
 * @param maxPosicoesPorLote   limite de posições GPS por envio do app
 * @param toleranciaRelogio    quanto o relógio do celular pode estar adiantado
 */
@ConfigurationProperties("rotacerta.rota")
public record RotaProperties(@DefaultValue Origem origem,
                             @DefaultValue("1.3") double fatorDistanciaViaria,
                             @DefaultValue("25") double velocidadeMediaKmH,
                             @DefaultValue("5") int minutosPorParada,
                             @DefaultValue("60") int maxParadas,
                             @DefaultValue("500") int maxPosicoesPorLote,
                             @DefaultValue("5m") Duration toleranciaRelogio) {

    public record Origem(@DefaultValue("CD Fortaleza") String nome,
                         @DefaultValue("-3.731900") double latitude,
                         @DefaultValue("-38.526700") double longitude) {
    }
}
