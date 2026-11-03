package br.com.fametro.spring.transportadora.veiculo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.ZoneId;

/**
 * Configurações {@code rotacerta.frota.*}.
 *
 * @param fusoHorario                 fuso usado para transformar as datas do filtro (inicio/fim) em instantes
 * @param toleranciaConsumoPercentual quanto o km/l pode ficar abaixo da referência antes de virar alerta
 * @param periodoPadraoDias           período das métricas quando inicio/fim não são informados
 */
@ConfigurationProperties("rotacerta.frota")
public record FrotaProperties(@DefaultValue("America/Fortaleza") ZoneId fusoHorario,
                              @DefaultValue("15") BigDecimal toleranciaConsumoPercentual,
                              @DefaultValue("30") int periodoPadraoDias) {
}
