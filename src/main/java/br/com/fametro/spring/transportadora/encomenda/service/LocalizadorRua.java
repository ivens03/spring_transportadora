package br.com.fametro.spring.transportadora.encomenda.service;

import java.math.BigDecimal;
import java.util.Optional;

/** Nome da rua (e bairro) de uma coordenada. Vazio quando não sabe: o rastreio segue sem a rua. */
public interface LocalizadorRua {

    Optional<Rua> em(BigDecimal latitude, BigDecimal longitude);

    /** @param bairro pode ser nulo (nem todo ponto do mapa tem bairro cadastrado) */
    record Rua(String logradouro, String bairro) {
    }
}
