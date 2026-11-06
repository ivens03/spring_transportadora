package br.com.fametro.spring.transportadora.monitoramento.service;

import br.com.fametro.spring.transportadora.monitoramento.dto.response.ArquiteturaResponse;
import br.com.fametro.spring.transportadora.monitoramento.dto.response.ArquiteturaResponse.Ambiente;
import br.com.fametro.spring.transportadora.monitoramento.dto.response.ArquiteturaResponse.Camada;
import br.com.fametro.spring.transportadora.monitoramento.dto.response.ArquiteturaResponse.Mapa;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Monta o "Como funciona": o mapa vem de {@code arquitetura/arquitetura.json} (lido uma vez, na subida) e o
 * ambiente é medido a cada chamada — versão do Java, do Spring Boot, do Hibernate, do Postgres, a última
 * migração do Flyway e há quanto tempo a API está no ar.
 */
@Slf4j
@Service
public class ArquiteturaService {

    static final String ARQUIVO = "arquitetura/arquitetura.json";

    private final Mapa mapa;
    private final Environment env;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<Flyway> flyway;
    private final Clock clock;

    public ArquiteturaService(JsonMapper jsonMapper, Environment env, JdbcTemplate jdbc, ObjectProvider<Flyway> flyway,
                              Clock clock) throws IOException {
        this.env = env;
        this.jdbc = jdbc;
        this.flyway = flyway;
        this.clock = clock;
        try (InputStream in = new ClassPathResource(ARQUIVO).getInputStream()) {
            this.mapa = validar(jsonMapper.readValue(in, Mapa.class));
        }
    }

    public ArquiteturaResponse montar() {
        return new ArquiteturaResponse(ambiente(), mapa.camadas(), mapa.fluxos(), mapa.ferramentas());
    }

    private Ambiente ambiente() {
        Runtime rt = Runtime.getRuntime();
        String servidor = "%s %s · %d CPUs · %d MB para a JVM".formatted(System.getProperty("os.name"),
                System.getProperty("os.arch"), rt.availableProcessors(), rt.maxMemory() / (1024 * 1024));
        String perfis = String.join(", ", env.getActiveProfiles());
        return new Ambiente(perfis.isEmpty() ? "default" : perfis,
                Runtime.version() + " (" + System.getProperty("java.vendor") + ")",
                SpringBootVersion.getVersion(), org.hibernate.Version.getVersionString(), banco(), migracao(), servidor,
                Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()), clock.instant());
    }

    private String banco() {
        try {
            return "PostgreSQL " + jdbc.queryForObject("show server_version", String.class);
        } catch (RuntimeException e) {
            log.warn("Sem versão do banco: {}", e.toString());
            return "indisponível";
        }
    }

    private String migracao() {
        Flyway f = flyway.getIfAvailable();
        MigrationInfo atual = f == null ? null : f.info().current();
        return atual == null ? "—" : atual.getVersion() + " — " + atual.getDescription();
    }

    /** Falha na subida se um passo apontar para uma camada que não existe: o desenho quebraria nas telas. */
    static Mapa validar(Mapa m) {
        Set<String> ids = new HashSet<>();
        for (Camada c : m.camadas()) {
            if (!ids.add(c.id())) {
                throw new IllegalStateException(ARQUIVO + ": camada repetida " + c.id());
            }
        }
        m.fluxos().forEach(f -> f.passos().forEach(p -> {
            if (!ids.contains(p.de()) || !ids.contains(p.para())) {
                throw new IllegalStateException(ARQUIVO + ": fluxo " + f.id() + " cita camada inexistente ("
                        + p.de() + " → " + p.para() + ")");
            }
        }));
        return m;
    }
}
