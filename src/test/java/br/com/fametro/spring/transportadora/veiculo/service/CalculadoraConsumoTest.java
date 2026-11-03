package br.com.fametro.spring.transportadora.veiculo.service;

import br.com.fametro.spring.transportadora.veiculo.model.enums.SituacaoConsumo;
import br.com.fametro.spring.transportadora.veiculo.service.CalculadoraConsumo.Leitura;
import br.com.fametro.spring.transportadora.veiculo.service.CalculadoraConsumo.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Método do tanque cheio, sem Spring nem banco. */
class CalculadoraConsumoTest {

    private static final BigDecimal TOLERANCIA = new BigDecimal("15");

    private static Leitura l(int km, String litros, String valor) {
        return new Leitura(km, new BigDecimal(litros), new BigDecimal(valor));
    }

    @Test
    void semAbastecimentoOuComUmSoNaoTemKmPorLitro() {
        Resultado vazio = CalculadoraConsumo.calcular(List.of(), BigDecimal.TEN, TOLERANCIA);
        assertThat(vazio.situacao()).isEqualTo(SituacaoConsumo.DADOS_INSUFICIENTES);
        assertThat(vazio.precoMedioLitro()).isNull();
        assertThat(vazio.calculado()).isFalse();

        Resultado um = CalculadoraConsumo.calcular(List.of(l(1000, "40", "240")), BigDecimal.TEN, TOLERANCIA);
        assertThat(um.situacao()).isEqualTo(SituacaoConsumo.DADOS_INSUFICIENTES);
        assertThat(um.gastoTotal()).isEqualByComparingTo("240");
        assertThat(um.precoMedioLitro()).isEqualByComparingTo("6");
    }

    @Test
    void primeiroAbastecimentoSoEnchePontoDePartida() {
        // fora de ordem de propósito: a calculadora ordena pelo odômetro
        Resultado r = CalculadoraConsumo.calcular(
                List.of(l(1450, "45", "270"), l(1000, "50", "300"), l(1900, "45", "270")), BigDecimal.TEN, TOLERANCIA);

        assertThat(r.kmRodados()).isEqualTo(900);
        assertThat(r.litrosAbastecidos()).isEqualByComparingTo("140");       // todos
        assertThat(r.litrosConsumidos()).isEqualByComparingTo("90");         // sem os 50 l do 1º
        assertThat(r.consumoMedioKmPorLitro()).isEqualByComparingTo("10.00");
        assertThat(r.custoPorKm()).isEqualByComparingTo("0.60");             // 540 / 900
        assertThat(r.desvioPercentual()).isEqualByComparingTo("0");
        assertThat(r.situacao()).isEqualTo(SituacaoConsumo.DENTRO_DO_ESPERADO);
    }

    @Test
    void toleranciaDe15PorCentoDefineOAlerta() {
        // referência 10 km/l: 8,5 km/l = exatamente −15% (ainda dentro); 8,4 km/l = −16% (alerta)
        Resultado limite = CalculadoraConsumo.calcular(List.of(l(0, "10", "60"), l(850, "100", "600")),
                BigDecimal.TEN, TOLERANCIA);
        assertThat(limite.desvioPercentual()).isEqualByComparingTo("-15");
        assertThat(limite.situacao()).isEqualTo(SituacaoConsumo.DENTRO_DO_ESPERADO);

        Resultado alerta = CalculadoraConsumo.calcular(List.of(l(0, "10", "60"), l(840, "100", "600")),
                BigDecimal.TEN, TOLERANCIA);
        assertThat(alerta.desvioPercentual()).isEqualByComparingTo("-16");
        assertThat(alerta.situacao()).isEqualTo(SituacaoConsumo.CONSUMO_ACIMA_DO_ESPERADO);
    }

    @Test
    void rendendoMaisQueAReferenciaEstaDentroDoEsperado() {
        Resultado r = CalculadoraConsumo.calcular(List.of(l(0, "10", "60"), l(1200, "100", "600")),
                BigDecimal.TEN, TOLERANCIA);
        assertThat(r.desvioPercentual()).isEqualByComparingTo("20");
        assertThat(r.situacao()).isEqualTo(SituacaoConsumo.DENTRO_DO_ESPERADO);
    }

    @Test
    void semReferenciaCalculaMasNaoClassifica() {
        Resultado r = CalculadoraConsumo.calcular(List.of(l(0, "10", "60"), l(300, "30", "180")), null, TOLERANCIA);
        assertThat(r.consumoMedioKmPorLitro()).isEqualByComparingTo("10");
        assertThat(r.desvioPercentual()).isNull();
        assertThat(r.situacao()).isEqualTo(SituacaoConsumo.SEM_REFERENCIA);
    }

    @Test
    void odometroParadoNaoDivideporZero() {
        Resultado r = CalculadoraConsumo.calcular(List.of(l(500, "10", "60"), l(500, "5", "30")), BigDecimal.TEN, TOLERANCIA);
        assertThat(r.situacao()).isEqualTo(SituacaoConsumo.DADOS_INSUFICIENTES);
        assertThat(r.consumoMedioKmPorLitro()).isNull();
    }
}
