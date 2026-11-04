package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.carga.repository.CargaRepository;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EncomendaResumoResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.EventoEncomendaResponse;
import br.com.fametro.spring.transportadora.encomenda.repository.EventoEncomendaRepository;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Portal do Cliente: o cliente só enxerga as próprias encomendas (a de outro cliente responde 404). */
@Service
@RequiredArgsConstructor
public class EncomendaService {

    private final CargaRepository cargaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoEncomendaRepository eventoRepository;
    private final RastreioEncomendaService rastreioService;

    @Transactional(readOnly = true)
    public Page<EncomendaResumoResponse> listar(String matriculaCliente, StatusCarga status, Pageable pageable) {
        return cargaRepository.findAll(CargaRepository.doCliente(cliente(matriculaCliente).getId(), status), pageable)
                .map(EncomendaResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public EncomendaResponse buscar(String matriculaCliente, String codigo) {
        Carga carga = carregar(matriculaCliente, codigo);
        return new EncomendaResponse(EncomendaResumoResponse.de(carga), rastreioService.rastrear(carga).orElse(null),
                eventoRepository.findByCargaIdOrderByOcorridoEmAscIdAsc(carga.getId()).stream()
                        .map(EventoEncomendaResponse::de).toList());
    }

    private Carga carregar(String matriculaCliente, String codigo) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        return cargaRepository.findByCodigoAndClienteId(c, cliente(matriculaCliente).getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encomenda " + c + " não encontrada."));
    }

    private Usuario cliente(String matricula) {
        return usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + matricula + " não encontrado."));
    }
}
