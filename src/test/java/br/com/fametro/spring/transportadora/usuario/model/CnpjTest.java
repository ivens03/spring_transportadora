package br.com.fametro.spring.transportadora.usuario.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjTest {

    @Test
    void numericoComESemPontuacao() {
        assertThat(Cnpj.valido("11.222.333/0001-81")).isTrue();
        assertThat(Cnpj.valido("11222333000181")).isTrue();
        assertThat(Cnpj.valido("11222333000182")).isFalse();
    }

    @Test
    void alfanumericoDaReceita() {
        // exemplo oficial da Receita Federal para o CNPJ alfanumérico
        assertThat(Cnpj.valido("12.ABC.345/01DE-35")).isTrue();
        assertThat(Cnpj.valido("12abc34501de35")).isTrue();
        assertThat(Cnpj.valido("12ABC34501DE36")).isFalse();
    }

    @Test
    void formatoErradoOuRepetido() {
        assertThat(Cnpj.valido("123")).isFalse();
        assertThat(Cnpj.valido("00000000000000")).isFalse();
        assertThat(Cnpj.valido("12ABC34501DEAB")).isFalse(); // dígitos verificadores são sempre números
        assertThat(Cnpj.valido(null)).isFalse();
    }

    @Test
    void formata() {
        assertThat(Cnpj.formatar("11222333000181")).isEqualTo("11.222.333/0001-81");
        assertThat(Cnpj.formatar("12ABC34501DE35")).isEqualTo("12.ABC.345/01DE-35");
    }
}
