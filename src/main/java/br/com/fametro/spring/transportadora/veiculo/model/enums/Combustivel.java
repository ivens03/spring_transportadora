package br.com.fametro.spring.transportadora.veiculo.model.enums;

/** Combustível do veículo. FLEX aceita abastecimento com GASOLINA ou ETANOL. */
public enum Combustivel {
    GASOLINA,
    ETANOL,
    FLEX,
    DIESEL;

    /** O veículo com este combustível pode ser abastecido com {@code abastecido}? */
    public boolean aceita(Combustivel abastecido) {
        if (abastecido == FLEX) {
            return false; // no abastecimento informa-se o que entrou no tanque
        }
        return this == FLEX ? (abastecido == GASOLINA || abastecido == ETANOL) : this == abastecido;
    }
}
