package br.com.fametro.spring.transportadora.veiculo.model.enums;

/** Leitura rápida para o Financeiro: o veículo está gastando dentro do esperado? */
public enum SituacaoConsumo {
    /** km/l dentro da tolerância em relação ao consumo de referência. */
    DENTRO_DO_ESPERADO,
    /** km/l pior que a referência além da tolerância (ex.: 15%): investigar manutenção, rota ou desvio. */
    CONSUMO_ACIMA_DO_ESPERADO,
    /** Veículo sem consumo de referência cadastrado: não dá para comparar. */
    SEM_REFERENCIA,
    /** Menos de 2 abastecimentos no período: não dá para calcular km/l. */
    DADOS_INSUFICIENTES
}
