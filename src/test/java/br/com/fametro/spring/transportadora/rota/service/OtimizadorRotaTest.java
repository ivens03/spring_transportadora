package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.rota.service.Geo.Ponto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Ordem das paradas: vizinho mais próximo + 2-opt (sem Spring nem banco). */
class OtimizadorRotaTest {

    private static final Ponto ORIGEM = new Ponto(0, 0);

    @Test
    void haversineDeUmGrauDeLatitudeDaUns111Km() {
        assertThat(Geo.distanciaKm(new Ponto(0, 0), new Ponto(1, 0))).isCloseTo(111.2, within(0.1));
        assertThat(Geo.distanciaKm(new Ponto(-3.73, -38.52), new Ponto(-3.73, -38.52))).isZero();
    }

    @Test
    void paradasEmLinhaSaoVisitadasDaMaisPertoParaAMaisLonge() {
        // cadastradas fora de ordem: longe, perto, meio
        var destinos = List.of(new Ponto(0, 0.03), new Ponto(0, 0.01), new Ponto(0, 0.02));
        var plano = OtimizadorRota.otimizar(ORIGEM, destinos, false);

        assertThat(plano.ordem()).containsExactly(1, 2, 0);
        double umTrecho = Geo.distanciaKm(ORIGEM, new Ponto(0, 0.01));
        assertThat(plano.distanciasKm()).allSatisfy(d -> assertThat(d).isCloseTo(umTrecho, within(1e-6)));
        assertThat(plano.retornoKm()).isZero();
        assertThat(plano.totalKm()).isCloseTo(3 * umTrecho, within(1e-6));
    }

    @Test
    void comRetornoSomaAVoltaAOrigem() {
        var plano = OtimizadorRota.otimizar(ORIGEM, List.of(new Ponto(0, 0.02), new Ponto(0, 0.01)), true);
        double umTrecho = Geo.distanciaKm(ORIGEM, new Ponto(0, 0.01));
        assertThat(plano.retornoKm()).isCloseTo(2 * umTrecho, within(1e-6));
        assertThat(plano.totalKm()).isCloseTo(4 * umTrecho, within(1e-6));
    }

    @Test
    void doisOptNuncaPioraOVizinhoMaisProximoEVisitaTodasUmaVez() {
        Random r = new Random(42);
        for (int caso = 0; caso < 20; caso++) {
            List<Ponto> destinos = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                destinos.add(new Ponto(-3.8 + r.nextDouble() * 0.15, -38.6 + r.nextDouble() * 0.15));
            }
            var plano = OtimizadorRota.otimizar(new Ponto(-3.73, -38.52), destinos, true);

            assertThat(new HashSet<>(plano.ordem())).hasSize(25);
            assertThat(plano.totalKm()).isLessThanOrEqualTo(vizinhoMaisProximo(new Ponto(-3.73, -38.52), destinos) + 1e-9);
            double soma = plano.distanciasKm().stream().mapToDouble(Double::doubleValue).sum() + plano.retornoKm();
            assertThat(plano.totalKm()).isCloseTo(soma, within(1e-9));
        }
    }

    @Test
    void semParadasDevolvePlanoVazio() {
        var plano = OtimizadorRota.otimizar(ORIGEM, List.of(), true);
        assertThat(plano.ordem()).isEmpty();
        assertThat(plano.totalKm()).isZero();
    }

    /** Referência: só o vizinho mais próximo, fechado na origem. */
    private static double vizinhoMaisProximo(Ponto origem, List<Ponto> destinos) {
        List<Ponto> restantes = new ArrayList<>(destinos);
        Ponto atual = origem;
        double total = 0;
        while (!restantes.isEmpty()) {
            Ponto de = atual;
            Ponto prox = restantes.stream()
                    .min((a, b) -> Double.compare(Geo.distanciaKm(de, a), Geo.distanciaKm(de, b))).orElseThrow();
            total += Geo.distanciaKm(atual, prox);
            restantes.remove(prox);
            atual = prox;
        }
        return total + Geo.distanciaKm(atual, origem);
    }
}
