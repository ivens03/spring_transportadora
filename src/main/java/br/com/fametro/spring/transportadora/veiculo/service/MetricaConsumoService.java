package br.com.fametro.spring.transportadora.veiculo.service;

import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.veiculo.config.FrotaProperties;
import br.com.fametro.spring.transportadora.veiculo.dto.response.MetricaConsumoResponse;
import br.com.fametro.spring.transportadora.veiculo.dto.response.MetricaFrotaResponse;
import br.com.fametro.spring.transportadora.veiculo.model.Abastecimento;
import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.SituacaoConsumo;
import br.com.fametro.spring.transportadora.veiculo.repository.AbastecimentoRepository;
import br.com.fametro.spring.transportadora.veiculo.repository.VeiculoRepository;
import br.com.fametro.spring.transportadora.veiculo.service.CalculadoraConsumo.Leitura;
import br.com.fametro.spring.transportadora.veiculo.service.CalculadoraConsumo.Resultado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Métricas de consumo da frota para o FINANCEIRO (km/l, R$/km, gasto e alerta de consumo). */
@Service
@RequiredArgsConstructor
public class MetricaConsumoService {

    /** Evita consulta gigante sem querer. */
    private static final int PERIODO_MAXIMO_DIAS = 366;

    private final VeiculoRepository veiculoRepository;
    private final AbastecimentoRepository abastecimentoRepository;
    private final VeiculoService veiculoService;
    private final FrotaProperties frotaProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MetricaConsumoResponse doVeiculo(Long veiculoId, LocalDate inicio, LocalDate fim) {
        Periodo periodo = periodo(inicio, fim);
        Veiculo veiculo = veiculoService.carregar(veiculoId);
        List<Abastecimento> abastecimentos =
                abastecimentoRepository.doVeiculoNoPeriodo(veiculoId, periodo.de(), periodo.ate());
        return montar(veiculo, periodo, calcular(veiculo, abastecimentos));
    }

    @Transactional(readOnly = true)
    public MetricaFrotaResponse daFrota(LocalDate inicio, LocalDate fim) {
        Periodo periodo = periodo(inicio, fim);
        Map<Long, List<Abastecimento>> porVeiculo = abastecimentoRepository
                .daFrotaNoPeriodo(periodo.de(), periodo.ate()).stream()
                .collect(Collectors.groupingBy(a -> a.getVeiculo().getId()));

        List<MetricaConsumoResponse> itens = new ArrayList<>();
        int abastecidos = 0;
        int acimaDoEsperado = 0;
        long km = 0;
        BigDecimal litros = BigDecimal.ZERO;
        BigDecimal gasto = BigDecimal.ZERO;
        BigDecimal litrosConsumidos = BigDecimal.ZERO;
        BigDecimal custoConsumido = BigDecimal.ZERO;

        for (Veiculo veiculo : veiculoRepository.findAllByOrderByPlacaAsc()) {
            List<Abastecimento> abastecimentos = porVeiculo.getOrDefault(veiculo.getId(), List.of());
            Resultado r = calcular(veiculo, abastecimentos);
            itens.add(montar(veiculo, periodo, r));
            if (!abastecimentos.isEmpty()) {
                abastecidos++;
            }
            if (r.situacao() == SituacaoConsumo.CONSUMO_ACIMA_DO_ESPERADO) {
                acimaDoEsperado++;
            }
            litros = litros.add(r.litrosAbastecidos());
            gasto = gasto.add(r.gastoTotal());
            if (r.calculado()) { // só entra no km/l da frota quem tem km/l próprio
                km += r.kmRodados();
                litrosConsumidos = litrosConsumidos.add(r.litrosConsumidos());
                custoConsumido = custoConsumido.add(r.custoConsumido());
            }
        }

        BigDecimal kmDecimal = BigDecimal.valueOf(km);
        BigDecimal consumoFrota = km > 0 ? kmDecimal.divide(litrosConsumidos, 2, RoundingMode.HALF_UP) : null;
        BigDecimal custoFrota = km > 0 ? custoConsumido.divide(kmDecimal, 2, RoundingMode.HALF_UP) : null;
        return new MetricaFrotaResponse(periodo.inicio(), periodo.fim(), abastecidos, acimaDoEsperado, km, litros,
                gasto, consumoFrota, custoFrota, itens);
    }

    private Resultado calcular(Veiculo veiculo, List<Abastecimento> abastecimentos) {
        List<Leitura> leituras = abastecimentos.stream()
                .map(a -> new Leitura(a.getOdometroKm(), a.getLitros(), a.getValorTotal()))
                .toList();
        return CalculadoraConsumo.calcular(leituras, veiculo.getConsumoReferenciaKmPorLitro(),
                frotaProperties.toleranciaConsumoPercentual());
    }

    private static MetricaConsumoResponse montar(Veiculo v, Periodo p, Resultado r) {
        return new MetricaConsumoResponse(v.getId(), v.getPlaca(), v.getModelo(), v.getTipo(), p.inicio(), p.fim(),
                r.quantidadeAbastecimentos(), r.kmRodados(), r.litrosAbastecidos(), r.gastoTotal(),
                r.precoMedioLitro(), r.consumoMedioKmPorLitro(), r.custoPorKm(), v.getConsumoReferenciaKmPorLitro(),
                r.desvioPercentual(), r.situacao());
    }

    /** Datas do filtro (inclusivas, no fuso da frota) → intervalo [de, ate) em instantes. */
    private Periodo periodo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock.withZone(frotaProperties.fusoHorario()));
        LocalDate f = fim != null ? fim : hoje;
        LocalDate i = inicio != null ? inicio : f.minusDays(frotaProperties.periodoPadraoDias() - 1L);
        if (i.isAfter(f)) {
            throw new RegraNegocioException("O início (" + i + ") não pode ser depois do fim (" + f + ").");
        }
        if (ChronoUnit.DAYS.between(i, f) >= PERIODO_MAXIMO_DIAS) {
            throw new RegraNegocioException("Período máximo de " + PERIODO_MAXIMO_DIAS + " dias.");
        }
        return new Periodo(i, f,
                i.atStartOfDay(frotaProperties.fusoHorario()).toInstant(),
                f.plusDays(1).atStartOfDay(frotaProperties.fusoHorario()).toInstant());
    }

    private record Periodo(LocalDate inicio, LocalDate fim, Instant de, Instant ate) {
    }
}
