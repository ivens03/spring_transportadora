package br.com.fametro.spring.transportadora.carga.service;

import br.com.fametro.spring.transportadora.carga.dto.request.CargaPatchRequest;
import br.com.fametro.spring.transportadora.carga.dto.request.CargaRequest;
import br.com.fametro.spring.transportadora.carga.dto.response.CargaResponse;
import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.carga.repository.CargaRepository;
import br.com.fametro.spring.transportadora.encomenda.evento.EncomendaAlterada;
import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.Perfil;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Cadastro de cargas pelo SUPORTE (permissão entrega:cadastrar). Carga com cliente vira "encomenda" no
 * Portal do Cliente: cadastro, troca de cliente e cancelamento publicam {@link EncomendaAlterada}.
 */
@Service
@RequiredArgsConstructor
public class CargaService {

    private final CargaRepository cargaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    @Transactional
    public CargaResponse cadastrar(CargaRequest r, String solicitante) {
        Usuario cliente = r.clienteMatricula() == null || r.clienteMatricula().isBlank()
                ? null : carregarCliente(r.clienteMatricula());
        String codigo = "CRG-%06d".formatted(cargaRepository.proximoNumeroCodigo());
        Instant agora = clock.instant();
        Carga carga = cargaRepository.save(Carga.nova(codigo, r.descricao().trim(), r.pesoKg(), r.volumeM3(),
                r.destinatarioNome().trim(), vazioParaNulo(r.destinatarioTelefone()), r.endereco().trim(),
                r.bairro().trim(), r.cidade().trim(), r.uf().toUpperCase(Locale.ROOT), vazioParaNulo(r.cep()),
                r.latitude(), r.longitude(), cliente, solicitante, agora));
        eventos.publishEvent(new EncomendaAlterada(carga, TipoEventoEncomenda.CADASTRADA, null, null, agora));
        return CargaResponse.de(carga);
    }

    @Transactional(readOnly = true)
    public Page<CargaResponse> listar(StatusCarga status, String cidade, String bairro, Pageable pageable) {
        return cargaRepository.findAll(CargaRepository.filtro(status, cidade, bairro), pageable).map(CargaResponse::de);
    }

    @Transactional(readOnly = true)
    public CargaResponse buscar(Long id) {
        return CargaResponse.de(carregar(id));
    }

    @Transactional
    public CargaResponse atualizar(Long id, CargaPatchRequest r) {
        if (r.vazio()) {
            throw new RegraNegocioException("Nada para atualizar: envie ao menos um campo.");
        }
        Carga carga = carregar(id);
        if (!carga.disponivelParaRota()) {
            throw new RegraNegocioException("A carga " + carga.getCodigo() + " está " + carga.getStatus()
                    + ": só dá para alterar enquanto AGUARDANDO_ROTA.");
        }
        Instant agora = clock.instant();
        if (r.descricao() != null) carga.alterarDescricao(obrigatorio(r.descricao(), "descricao"), agora);
        if (r.pesoKg() != null) carga.alterarPesoKg(r.pesoKg(), agora);
        if (r.volumeM3() != null) carga.alterarVolumeM3(r.volumeM3(), agora);
        if (r.destinatarioNome() != null) carga.alterarDestinatarioNome(obrigatorio(r.destinatarioNome(), "destinatarioNome"), agora);
        if (r.destinatarioTelefone() != null) carga.alterarDestinatarioTelefone(vazioParaNulo(r.destinatarioTelefone()), agora);
        if (r.mexeNoEndereco()) {
            if (!r.enderecoCompleto()) {
                throw new RegraNegocioException("Para mudar o destino envie endereco, bairro, cidade, uf, latitude e longitude juntos.");
            }
            carga.alterarEndereco(obrigatorio(r.endereco(), "endereco"), obrigatorio(r.bairro(), "bairro"),
                    obrigatorio(r.cidade(), "cidade"), r.uf().toUpperCase(Locale.ROOT), vazioParaNulo(r.cep()),
                    r.latitude(), r.longitude(), agora);
        }
        if (r.clienteMatricula() != null) {
            Usuario novo = r.clienteMatricula().isBlank() ? null : carregarCliente(r.clienteMatricula());
            boolean mudou = novo == null ? carga.getCliente() != null
                    : carga.getCliente() == null || !carga.getCliente().getId().equals(novo.getId());
            carga.vincularCliente(novo, agora);
            if (mudou && novo != null) {
                // para o novo dono, a encomenda "chegou" agora
                eventos.publishEvent(new EncomendaAlterada(carga, TipoEventoEncomenda.CADASTRADA, null, null, agora));
            }
        }
        if (r.status() != null) {
            if (r.status() != StatusCarga.CANCELADA) {
                throw new RegraNegocioException("status aceita apenas CANCELADA; os outros mudam pela rota.");
            }
            carga.cancelar(agora);
            eventos.publishEvent(new EncomendaAlterada(carga, TipoEventoEncomenda.CANCELADA, null, null, agora));
        }
        return CargaResponse.de(carga);
    }

    private Carga carregar(Long id) {
        return cargaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carga " + id + " não encontrada."));
    }

    private Usuario carregarCliente(String matricula) {
        String m = Usuario.normalizarMatricula(matricula);
        Usuario cliente = usuarioRepository.findByMatricula(m)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + m + " não encontrado."));
        if (cliente.getPerfil() != Perfil.CLIENTE) {
            throw new RegraNegocioException(m + " não é cliente (perfil " + cliente.getPerfil() + ").");
        }
        return cliente;
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor.isBlank()) {
            throw new RegraNegocioException("O campo " + campo + " não pode ficar em branco.");
        }
        return valor.trim();
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
