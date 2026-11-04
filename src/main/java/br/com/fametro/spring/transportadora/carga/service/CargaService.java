package br.com.fametro.spring.transportadora.carga.service;

import br.com.fametro.spring.transportadora.carga.dto.request.CargaPatchRequest;
import br.com.fametro.spring.transportadora.carga.dto.request.CargaRequest;
import br.com.fametro.spring.transportadora.carga.dto.response.CargaResponse;
import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.carga.repository.CargaRepository;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/** Cadastro de cargas pelo SUPORTE (permissão entrega:cadastrar). */
@Service
@RequiredArgsConstructor
public class CargaService {

    private final CargaRepository cargaRepository;
    private final Clock clock;

    @Transactional
    public CargaResponse cadastrar(CargaRequest r, String solicitante) {
        String codigo = "CRG-%06d".formatted(cargaRepository.proximoNumeroCodigo());
        Carga carga = Carga.nova(codigo, r.descricao().trim(), r.pesoKg(), r.volumeM3(), r.destinatarioNome().trim(),
                vazioParaNulo(r.destinatarioTelefone()), r.endereco().trim(), r.bairro().trim(), r.cidade().trim(),
                r.uf().toUpperCase(Locale.ROOT), vazioParaNulo(r.cep()), r.latitude(), r.longitude(),
                solicitante, clock.instant());
        return CargaResponse.de(cargaRepository.save(carga));
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
        if (r.status() != null) {
            if (r.status() != StatusCarga.CANCELADA) {
                throw new RegraNegocioException("status aceita apenas CANCELADA; os outros mudam pela rota.");
            }
            carga.cancelar(agora);
        }
        return CargaResponse.de(carga);
    }

    private Carga carregar(Long id) {
        return cargaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carga " + id + " não encontrada."));
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
