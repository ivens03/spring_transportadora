package br.com.fametro.spring.transportadora.veiculo.service;

import br.com.fametro.spring.transportadora.veiculo.model.enums.SituacaoConsumo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Cálculo de consumo pelo método do tanque cheio (sem banco, sem Spring: testável isoladamente).
 * <p>
 * Com os abastecimentos de um período ordenados pelo odômetro:
 * <ul>
 *   <li>km rodados = último odômetro − primeiro odômetro;</li>
 *   <li>litros consumidos = soma dos litros a partir do 2º abastecimento (o 1º só enche o tanque de partida);</li>
 *   <li>km/l = km rodados / litros consumidos; R$/km = valor desses mesmos abastecimentos / km rodados;</li>
 *   <li>desvio = (km/l − referência) / referência × 100; abaixo de −tolerância → CONSUMO_ACIMA_DO_ESPERADO.</li>
 * </ul>
 */
public final class CalculadoraConsumo {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraConsumo() {
    }

    /** Dados mínimos de um abastecimento para o cálculo. */
    public record Leitura(int odometroKm, BigDecimal litros, BigDecimal valorTotal) {
    }

    public record Resultado(int quantidadeAbastecimentos, int kmRodados, BigDecimal litrosAbastecidos,
                            BigDecimal gastoTotal, BigDecimal precoMedioLitro,
                            /** Base do km/l: litros e valor sem o 1º abastecimento. */
                            BigDecimal litrosConsumidos, BigDecimal custoConsumido,
                            BigDecimal consumoMedioKmPorLitro, BigDecimal custoPorKm,
                            BigDecimal desvioPercentual, SituacaoConsumo situacao) {

        /** Tem km/l calculado (≥ 2 abastecimentos e km &gt; 0)? */
        public boolean calculado() {
            return consumoMedioKmPorLitro != null;
        }
    }

    public static Resultado calcular(List<Leitura> leituras, BigDecimal consumoReferencia,
                                     BigDecimal toleranciaPercentual) {
        List<Leitura> ordenadas = leituras.stream().sorted(Comparator.comparingInt(Leitura::odometroKm)).toList();
        int quantidade = ordenadas.size();
        BigDecimal litros = somar(ordenadas, Leitura::litros);
        BigDecimal gasto = somar(ordenadas, Leitura::valorTotal);
        BigDecimal precoMedio = litros.signum() > 0 ? gasto.divide(litros, 3, RoundingMode.HALF_UP) : null;

        if (quantidade < 2) {
            return new Resultado(quantidade, 0, litros, gasto, precoMedio, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, null, SituacaoConsumo.DADOS_INSUFICIENTES);
        }

        int km = ordenadas.getLast().odometroKm() - ordenadas.getFirst().odometroKm();
        List<Leitura> consumidas = ordenadas.subList(1, quantidade);
        BigDecimal litrosConsumidos = somar(consumidas, Leitura::litros);
        BigDecimal custoConsumido = somar(consumidas, Leitura::valorTotal);
        if (km <= 0) {
            return new Resultado(quantidade, 0, litros, gasto, precoMedio, litrosConsumidos, custoConsumido,
                    null, null, null, SituacaoConsumo.DADOS_INSUFICIENTES);
        }

        BigDecimal kmDecimal = BigDecimal.valueOf(km);
        BigDecimal kmPorLitro = kmDecimal.divide(litrosConsumidos, 2, RoundingMode.HALF_UP);
        BigDecimal custoPorKm = custoConsumido.divide(kmDecimal, 2, RoundingMode.HALF_UP);

        BigDecimal desvio = null;
        SituacaoConsumo situacao = SituacaoConsumo.SEM_REFERENCIA;
        if (consumoReferencia != null && consumoReferencia.signum() > 0) {
            // usa o km/l sem arredondar para o desvio não herdar erro de arredondamento
            BigDecimal exato = kmDecimal.divide(litrosConsumidos, 10, RoundingMode.HALF_UP);
            desvio = exato.subtract(consumoReferencia).multiply(CEM).divide(consumoReferencia, 2, RoundingMode.HALF_UP);
            situacao = desvio.compareTo(toleranciaPercentual.negate()) < 0
                    ? SituacaoConsumo.CONSUMO_ACIMA_DO_ESPERADO
                    : SituacaoConsumo.DENTRO_DO_ESPERADO;
        }
        return new Resultado(quantidade, km, litros, gasto, precoMedio, litrosConsumidos, custoConsumido,
                kmPorLitro, custoPorKm, desvio, situacao);
    }

    private static BigDecimal somar(List<Leitura> leituras, java.util.function.Function<Leitura, BigDecimal> campo) {
        return leituras.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
