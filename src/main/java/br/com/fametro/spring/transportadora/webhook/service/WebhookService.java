package br.com.fametro.spring.transportadora.webhook.service;

import br.com.fametro.spring.transportadora.encomenda.model.enums.TipoEventoEncomenda;
import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.repository.UsuarioRepository;
import br.com.fametro.spring.transportadora.webhook.config.WebhookProperties;
import br.com.fametro.spring.transportadora.webhook.dto.request.WebhookPatchRequest;
import br.com.fametro.spring.transportadora.webhook.dto.request.WebhookRequest;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookEntregaResponse;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookPayload;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookResponse;
import br.com.fametro.spring.transportadora.webhook.dto.response.WebhookSegredoResponse;
import br.com.fametro.spring.transportadora.webhook.model.Webhook;
import br.com.fametro.spring.transportadora.webhook.model.WebhookEntrega;
import br.com.fametro.spring.transportadora.webhook.model.enums.StatusEntregaWebhook;
import br.com.fametro.spring.transportadora.webhook.repository.WebhookEntregaRepository;
import br.com.fametro.spring.transportadora.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** Webhooks do cliente (permissão webhook:gerenciar): cada um só mexe nos próprios (os de outro = 404). */
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookEntregaRepository entregaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorUrlWebhook validadorUrl;
    private final EnfileiradorWebhook enfileirador;
    private final WebhookProperties props;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional
    public WebhookSegredoResponse criar(String matriculaCliente, WebhookRequest r) {
        Usuario cliente = cliente(matriculaCliente);
        String url = validadorUrl.validar(r.url()).toString();
        if (webhookRepository.countByClienteId(cliente.getId()) >= props.maxPorCliente()) {
            throw new RegraNegocioException("Limite de " + props.maxPorCliente() + " webhooks por cliente.");
        }
        if (webhookRepository.existsByClienteIdAndUrl(cliente.getId(), url)) {
            throw new ConflitoException("Já existe webhook para esta URL.");
        }
        Webhook w = Webhook.novo(cliente, url, AssinaturaWebhook.gerarSegredo(), eventos(r.eventos()), clock.instant());
        return WebhookSegredoResponse.de(webhookRepository.save(w));
    }

    @Transactional(readOnly = true)
    public Page<WebhookResponse> listar(String matriculaCliente, Pageable pageable) {
        return webhookRepository.findByClienteId(cliente(matriculaCliente).getId(), pageable).map(WebhookResponse::de);
    }

    @Transactional(readOnly = true)
    public WebhookResponse buscar(String matriculaCliente, Long id) {
        return WebhookResponse.de(carregar(matriculaCliente, id));
    }

    @Transactional
    public WebhookResponse atualizar(String matriculaCliente, Long id, WebhookPatchRequest r) {
        if (r.vazio()) {
            throw new RegraNegocioException("Nada para atualizar: envie url, eventos e/ou ativo.");
        }
        Webhook w = carregar(matriculaCliente, id);
        Instant agora = clock.instant();
        if (r.url() != null) {
            String url = validadorUrl.validar(r.url()).toString();
            if (!url.equals(w.getUrl()) && webhookRepository.existsByClienteIdAndUrl(w.getCliente().getId(), url)) {
                throw new ConflitoException("Já existe webhook para esta URL.");
            }
            w.alterarUrl(url, agora);
        }
        if (r.eventos() != null) {
            w.alterarEventos(eventos(r.eventos()), agora);
        }
        if (r.ativo() != null) {
            w.alterarAtivo(r.ativo(), agora);
        }
        return WebhookResponse.de(w);
    }

    /** Exclui o webhook e o histórico de avisos dele (os pendentes deixam de ser enviados). */
    @Transactional
    public void excluir(String matriculaCliente, Long id) {
        webhookRepository.delete(carregar(matriculaCliente, id));
    }

    /** Segredo vazou ou rotação periódica: o antigo deixa de valer na hora. */
    @Transactional
    public WebhookSegredoResponse trocarSegredo(String matriculaCliente, Long id) {
        Webhook w = carregar(matriculaCliente, id);
        w.trocarSegredo(AssinaturaWebhook.gerarSegredo(), clock.instant());
        return WebhookSegredoResponse.de(w);
    }

    /** Coloca um PING na fila (sai na próxima rodada do despachante, em segundos). */
    @Transactional
    public WebhookEntregaResponse ping(String matriculaCliente, Long id) {
        Webhook w = carregar(matriculaCliente, id);
        if (!w.isAtivo()) {
            throw new RegraNegocioException("Webhook pausado (ativo=false): reative para testar.");
        }
        return resposta(enfileirador.ping(w));
    }

    @Transactional(readOnly = true)
    public Page<WebhookEntregaResponse> entregas(String matriculaCliente, Long id, StatusEntregaWebhook status, Pageable pageable) {
        Webhook w = carregar(matriculaCliente, id);
        Page<WebhookEntrega> pagina = status == null
                ? entregaRepository.findByWebhookId(w.getId(), pageable)
                : entregaRepository.findByWebhookIdAndStatus(w.getId(), status, pageable);
        return pagina.map(this::resposta);
    }

    private WebhookEntregaResponse resposta(WebhookEntrega e) {
        return WebhookEntregaResponse.de(e, jsonMapper.readValue(e.getPayload(), WebhookPayload.class));
    }

    /** Vazio = todos os assináveis. PING não se assina (sempre vai quando o cliente pede o teste). */
    private static Set<TipoEventoEncomenda> eventos(Set<TipoEventoEncomenda> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            Set<TipoEventoEncomenda> todos = EnumSet.noneOf(TipoEventoEncomenda.class);
            Arrays.stream(TipoEventoEncomenda.values()).filter(TipoEventoEncomenda::isAssinavel).forEach(todos::add);
            return todos;
        }
        if (pedidos.contains(null) || pedidos.stream().anyMatch(t -> !t.isAssinavel())) {
            throw new RegraNegocioException("PING não é assinável: use POST /api/v1/webhooks/{id}/pings para testar.");
        }
        return EnumSet.copyOf(pedidos);
    }

    private Webhook carregar(String matriculaCliente, Long id) {
        return webhookRepository.findByIdAndClienteId(id, cliente(matriculaCliente).getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Webhook " + id + " não encontrado."));
    }

    private Usuario cliente(String matricula) {
        return usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + matricula + " não encontrado."));
    }
}
