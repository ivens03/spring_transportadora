package br.com.fametro.spring.transportadora.seguranca.service;

import java.time.Instant;

/** Hash Argon2id recém-gerado e a data em que a chave de salt usada nele vence. */
public record CredencialGerada(String hash, Instant expiraEm) {
}
