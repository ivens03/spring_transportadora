package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.encomenda.dto.response.LocalizacaoResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.RastreioResponse;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** A frase do webhook de LOCALIZACAO: rua, quanto falta e quantas entregas antes. */
class ResumoRastreioTest {

    private static final Instant AGORA = Instant.parse("2026-11-05T12:00:00Z");

    private static RastreioResponse rastreio(String rua, String bairro, Integer antes, Integer minutos, Instant chegada) {
        LocalizacaoResponse loc = new LocalizacaoResponse(new BigDecimal("-3.78"), new BigDecimal("-38.57"),
                new BigDecimal("30"), AGORA, rua, bairro);
        return new RastreioResponse("ROT-000001", StatusRota.EM_ANDAMENTO, "Ana", "Fiorino", "RTC2C26", 1, antes, loc,
                new BigDecimal("3.36"), minutos, chegada, null);
    }

    @Test
    void comRuaEBairro() {
        assertThat(RastreioEncomendaService.resumo(rastreio("Avenida Bezerra de Menezes", "São Gerardo", 0, 12, AGORA)))
                .isEqualTo("Na Avenida Bezerra de Menezes, São Gerardo · faltam ~12 min (3,4 km) · é a próxima entrega");
    }

    @Test
    void semBairroComEntregasAntes() {
        assertThat(RastreioEncomendaService.resumo(rastreio("Rua Pedro de Queirós", null, 1, 18, AGORA)))
                .isEqualTo("Na Rua Pedro de Queirós · faltam ~18 min (3,4 km) · 1 entrega antes da sua");
        assertThat(RastreioEncomendaService.resumo(rastreio("Rua X", null, 3, 18, AGORA))).endsWith("3 entregas antes da sua");
    }

    @Test
    void semRuaEMaisDeUmaHora() {
        assertThat(RastreioEncomendaService.resumo(rastreio(null, null, 0, 65, AGORA)))
                .isEqualTo("Faltam ~1 h 05 min (3,4 km) · é a próxima entrega");
    }

    @Test
    void semPrevisaoNaoTemFrase() {
        assertThat(RastreioEncomendaService.resumo(rastreio("Rua X", null, 0, 12, null))).isNull(); // rota não saiu
        assertThat(RastreioEncomendaService.resumo(null)).isNull();
    }
}
