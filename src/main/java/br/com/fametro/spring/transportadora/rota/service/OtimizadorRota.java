package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.rota.service.Geo.Ponto;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a ordem das paradas (problema do caixeiro-viajante) em duas etapas:
 * <ol>
 *   <li><b>vizinho mais próximo</b>: sai da origem e vai sempre para a parada mais perto ainda não visitada;</li>
 *   <li><b>2-opt</b>: inverte trechos da sequência enquanto isso encurtar o total (desfaz "cruzamentos").</li>
 * </ol>
 * Não é o ótimo garantido (o problema é NP-difícil), mas para até ~60 paradas fica muito próximo e roda em
 * milissegundos. Usa distância em linha reta; a distância pelas ruas é estimada depois com um fator.
 */
public final class OtimizadorRota {

    private OtimizadorRota() {
    }

    /**
     * @param ordem            índices de {@code destinos} na ordem de visita
     * @param distanciasKm     linha reta de cada parada até a anterior (a 1ª é desde a origem), mesma ordem
     * @param retornoKm        linha reta da última parada de volta à origem (0 se não retorna)
     * @param totalKm          soma de tudo (inclui o retorno)
     */
    public record Plano(List<Integer> ordem, List<Double> distanciasKm, double retornoKm, double totalKm) {
    }

    public static Plano otimizar(Ponto origem, List<Ponto> destinos, boolean retornarAOrigem) {
        int n = destinos.size();
        if (n == 0) {
            return new Plano(List.of(), List.of(), 0, 0);
        }
        // matriz de distâncias: índice 0 = origem, 1..n = destinos
        List<Ponto> pontos = new ArrayList<>(n + 1);
        pontos.add(origem);
        pontos.addAll(destinos);
        double[][] d = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = i + 1; j <= n; j++) {
                d[i][j] = d[j][i] = Geo.distanciaKm(pontos.get(i), pontos.get(j));
            }
        }

        int[] seq = vizinhoMaisProximo(d, n);
        doisOpt(seq, d, retornarAOrigem);

        List<Integer> ordem = new ArrayList<>(n);
        List<Double> trechos = new ArrayList<>(n);
        int anterior = 0;
        double total = 0;
        for (int no : seq) {
            ordem.add(no - 1);
            trechos.add(d[anterior][no]);
            total += d[anterior][no];
            anterior = no;
        }
        double retorno = retornarAOrigem ? d[anterior][0] : 0;
        return new Plano(ordem, trechos, retorno, total + retorno);
    }

    private static int[] vizinhoMaisProximo(double[][] d, int n) {
        int[] seq = new int[n];
        boolean[] visitado = new boolean[n + 1];
        int atual = 0;
        for (int passo = 0; passo < n; passo++) {
            int melhor = -1;
            for (int j = 1; j <= n; j++) {
                if (!visitado[j] && (melhor == -1 || d[atual][j] < d[atual][melhor])) {
                    melhor = j;
                }
            }
            seq[passo] = melhor;
            visitado[melhor] = true;
            atual = melhor;
        }
        return seq;
    }

    /** Inverte seq[i..k] sempre que encurtar; repete até não melhorar mais. A origem fica fixa no início. */
    private static void doisOpt(int[] seq, double[][] d, boolean fechado) {
        int n = seq.length;
        boolean melhorou = true;
        while (melhorou) {
            melhorou = false;
            for (int i = 0; i < n - 1; i++) {
                for (int k = i + 1; k < n; k++) {
                    int antes = i == 0 ? 0 : seq[i - 1];
                    int depois = k == n - 1 ? (fechado ? 0 : -1) : seq[k + 1];
                    double atual = d[antes][seq[i]] + (depois >= 0 ? d[seq[k]][depois] : 0);
                    double novo = d[antes][seq[k]] + (depois >= 0 ? d[seq[i]][depois] : 0);
                    if (novo < atual - 1e-9) {
                        for (int a = i, b = k; a < b; a++, b--) {
                            int t = seq[a];
                            seq[a] = seq[b];
                            seq[b] = t;
                        }
                        melhorou = true;
                    }
                }
            }
        }
    }
}
