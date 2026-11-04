package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.encomenda.evento.EncomendaAlterada;
import br.com.fametro.spring.transportadora.encomenda.model.EventoEncomenda;
import br.com.fametro.spring.transportadora.encomenda.repository.EventoEncomendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Grava a linha do tempo da encomenda na mesma transação da mudança (só cargas com cliente). */
@Component
@RequiredArgsConstructor
class HistoricoEncomendaListener {

    private final EventoEncomendaRepository repository;

    @EventListener
    void aoAlterar(EncomendaAlterada e) {
        if (e.carga().getCliente() == null || !e.tipo().isHistorico()) {
            return;
        }
        repository.save(EventoEncomenda.novo(e.carga().getId(), e.tipo(), e.rotaCodigo(), e.detalhe(), e.ocorridoEm()));
    }
}
