package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.encomenda.config.RuaProperties;
import br.com.fametro.spring.transportadora.encomenda.service.LocalizadorRua.Rua;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** Rua pela posição: leitura do JSON, cache, limite de 1 consulta/s e pausa quando o serviço cai (sem internet). */
class NominatimLocalizadorRuaTest {

    private static final String BEZERRA = """
            {"address":{"road":"Avenida Bezerra de Menezes","suburb":"São Gerardo","city":"Fortaleza"}}""";

    /** Relógio que o teste avança na mão. */
    private static final class Relogio extends Clock {
        Instant agora = Instant.parse("2026-11-05T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private HttpServer servidor;
    private final List<String> consultas = new CopyOnWriteArrayList<>();
    private final List<String> agentes = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String resposta = BEZERRA;
    private final Relogio relogio = new Relogio();

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/reverse", troca -> {
            consultas.add(troca.getRequestURI().getQuery());
            agentes.add(troca.getRequestHeaders().getFirst("User-Agent"));
            byte[] corpo = resposta.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void derrubar() {
        servidor.stop(0);
    }

    private NominatimLocalizadorRua localizador(boolean ativo) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/reverse";
        RuaProperties props = new RuaProperties(ativo, url, "RotaCerta-teste", Duration.ofSeconds(2),
                Duration.ofSeconds(1), Duration.ofMinutes(1), 100);
        return new NominatimLocalizadorRua(props, JsonMapper.builder().build(), relogio);
    }

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void leRuaEBairroEMandaOUserAgent() {
        Optional<Rua> rua = localizador(true).em(d("-3.733676"), d("-38.558170"));

        assertThat(rua).contains(new Rua("Avenida Bezerra de Menezes", "São Gerardo"));
        assertThat(consultas.getFirst()).contains("lat=-3.7337", "lon=-38.5582", "zoom=17", "accept-language=pt-BR");
        assertThat(agentes.getFirst()).isEqualTo("RotaCerta-teste");
    }

    @Test
    void mesmaPosicaoArredondadaUsaOCacheEOutraRespeita1PorSegundo() {
        NominatimLocalizadorRua l = localizador(true);
        l.em(d("-3.733676"), d("-38.558170"));

        relogio.avancar(Duration.ofSeconds(5));
        assertThat(l.em(d("-3.733690"), d("-38.558150"))).isPresent(); // ~2 m de diferença: mesmo ponto do cache
        assertThat(consultas).hasSize(1);

        assertThat(l.em(d("-3.740000"), d("-38.560000"))).isPresent(); // ponto novo: consulta
        assertThat(l.em(d("-3.750000"), d("-38.570000"))).isEmpty();   // outro no mesmo segundo: não consulta
        assertThat(consultas).hasSize(2);

        relogio.avancar(Duration.ofSeconds(1));
        assertThat(l.em(d("-3.750000"), d("-38.570000"))).isPresent(); // passou 1 s: agora consulta
        assertThat(consultas).hasSize(3);
    }

    @Test
    void lugarSemRuaNoMapaViraVazioEFicaNoCache() {
        resposta = """
                {"address":{"city":"Fortaleza"}}""";
        NominatimLocalizadorRua l = localizador(true);

        assertThat(l.em(d("-3.70"), d("-38.50"))).isEmpty();
        relogio.avancar(Duration.ofSeconds(5));
        assertThat(l.em(d("-3.70"), d("-38.50"))).isEmpty();
        assertThat(consultas).hasSize(1);
    }

    @Test
    void servicoForaDoArPausaUmMinutoSemTentar() {
        status = 503;
        NominatimLocalizadorRua l = localizador(true);

        assertThat(l.em(d("-3.70"), d("-38.50"))).isEmpty();
        relogio.avancar(Duration.ofSeconds(30));
        status = 200;
        assertThat(l.em(d("-3.71"), d("-38.51"))).isEmpty(); // ainda na pausa
        assertThat(consultas).hasSize(1);

        relogio.avancar(Duration.ofSeconds(31));
        assertThat(l.em(d("-3.71"), d("-38.51"))).isPresent(); // voltou
        assertThat(consultas).hasSize(2);
    }

    @Test
    void desligadoNaoConsulta() {
        assertThat(localizador(false).em(d("-3.70"), d("-38.50"))).isEmpty();
        assertThat(consultas).isEmpty();
    }
}
