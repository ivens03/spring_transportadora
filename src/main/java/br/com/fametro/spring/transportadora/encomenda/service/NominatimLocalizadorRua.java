package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.encomenda.config.RuaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rua da posição do veículo pelo Nominatim (OpenStreetMap). Pensado para não atrapalhar quem recebe o GPS:
 * <ul>
 *   <li><b>cache</b> por coordenada arredondada a 4 casas (~11 m): o portal e o webhook da mesma posição
 *       fazem uma consulta só, e o carro parado no sinal não consulta de novo;</li>
 *   <li><b>no máximo 1 consulta por segundo</b> (regra de uso do Nominatim): passou disso, responde vazio na hora
 *       em vez de esperar;</li>
 *   <li><b>timeout curto</b> e, se o serviço cair, <b>pausa</b> de 1 min sem tentar.</li>
 * </ul>
 * Qualquer falha vira "sem rua": a previsão de chegada continua saindo normalmente.
 */
@Slf4j
@Service
class NominatimLocalizadorRua implements LocalizadorRua {

    private final RuaProperties props;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final HttpClient http;
    /** LRU: o mais antigo sai quando passa do limite. Optional.empty() guardado = "o mapa não tem rua aqui". */
    private final Map<String, Optional<Rua>> cache;

    private Instant ultimaConsulta = Instant.EPOCH;
    private Instant pausadoAte = Instant.EPOCH;

    NominatimLocalizadorRua(RuaProperties props, JsonMapper jsonMapper, Clock clock) {
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.http = HttpClient.newBuilder()
                .connectTimeout(props.timeout())
                .proxy(ProxySelector.getDefault()) // respeita -Dhttps.proxyHost (máquina atrás de proxy)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Optional<Rua>> mais) {
                return size() > props.maxCache();
            }
        });
    }

    @Override
    public Optional<Rua> em(BigDecimal latitude, BigDecimal longitude) {
        if (!props.ativo() || latitude == null || longitude == null) {
            return Optional.empty();
        }
        String lat = latitude.setScale(4, RoundingMode.HALF_UP).toPlainString();
        String lon = longitude.setScale(4, RoundingMode.HALF_UP).toPlainString();
        String chave = lat + "," + lon;
        Optional<Rua> guardada = cache.get(chave);
        if (guardada != null) {
            return guardada;
        }
        if (!podeConsultar()) {
            return Optional.empty(); // não guarda: na próxima posição tenta de novo
        }
        try {
            Optional<Rua> rua = consultar(lat, lon);
            cache.put(chave, rua);
            return rua;
        } catch (Exception e) {
            synchronized (this) {
                pausadoAte = clock.instant().plus(props.pausaAposFalha());
            }
            log.warn("Nominatim indisponível ({}); rastreio sem nome de rua por {}", e.toString(), props.pausaAposFalha());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private synchronized boolean podeConsultar() {
        Instant agora = clock.instant();
        if (agora.isBefore(pausadoAte) || agora.isBefore(ultimaConsulta.plus(props.intervaloMinimo()))) {
            return false;
        }
        ultimaConsulta = agora;
        return true;
    }

    private Optional<Rua> consultar(String lat, String lon) throws Exception {
        // zoom 17 = nível de rua; jsonv2 traz o endereço separado em partes
        URI uri = URI.create(props.url() + "?format=jsonv2&zoom=17&addressdetails=1&accept-language=pt-BR&lat=" + lat + "&lon=" + lon);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(props.timeout())
                .header("User-Agent", props.agente())
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        return ler(jsonMapper.readTree(resp.body()));
    }

    /** {@code address.road} (ou praça/calçadão) + o bairro mais próximo que o OpenStreetMap tiver. */
    static Optional<Rua> ler(JsonNode json) {
        JsonNode end = json.path("address");
        String rua = primeiro(end, "road", "pedestrian", "footway", "square", "residential");
        if (rua == null) {
            return Optional.empty();
        }
        return Optional.of(new Rua(rua, primeiro(end, "suburb", "neighbourhood", "quarter", "city_district")));
    }

    private static String primeiro(JsonNode no, String... campos) {
        for (String c : campos) {
            String v = no.path(c).asString("");
            if (!v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
