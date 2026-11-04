package br.com.fametro.spring.transportadora.webhook.model;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Eventos assinados ↔ coluna texto "ROTEIRIZADA,SAIU_PARA_ENTREGA,...". */
@Converter
class EventosConverter implements AttributeConverter<Set<TipoEventoEncomenda>, String> {

    @Override
    public String convertToDatabaseColumn(Set<TipoEventoEncomenda> eventos) {
        return eventos.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<TipoEventoEncomenda> convertToEntityAttribute(String coluna) {
        Set<TipoEventoEncomenda> eventos = EnumSet.noneOf(TipoEventoEncomenda.class);
        if (coluna != null && !coluna.isBlank()) {
            Arrays.stream(coluna.split(",")).map(String::trim).map(TipoEventoEncomenda::valueOf).forEach(eventos::add);
        }
        return eventos;
    }
}
