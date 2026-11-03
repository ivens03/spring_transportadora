package br.com.fametro.spring.transportadora.veiculo.model.enums;

public enum StatusVeiculo {
    ATIVO,
    EM_MANUTENCAO,
    /** Fora da frota: não recebe novos abastecimentos. */
    INATIVO
}
