package br.com.fametro.spring.transportadora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Beans gerais da aplicação. */
@Configuration
public class AppConfig {

    /** Relógio injetável (UTC): facilita testar regras com tempo (expiração, bloqueio). */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
