package br.com.fametro.spring.transportadora.webhook.service;

import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.webhook.config.WebhookProperties;
import br.com.fametro.spring.transportadora.webhook.model.WebhookEntrega;
import br.com.fametro.spring.transportadora.webhook.repository.FilaWebhookRepository;
import br.com.fametro.spring.transportadora.webhook.repository.WebhookEntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Envia os avisos da fila. Cada rodada: (1) reserva um lote numa transação curta; (2) faz os POSTs FORA de
 * transação (o servidor do cliente pode demorar, não seguramos conexão do banco); (3) grava o resultado de
 * cada um. 2xx = ENTREGUE; qualquer outra coisa = tenta de novo com espera dobrando, até desistir (FALHOU).
 * <p>Headers enviados:
 * <pre>
 * X-RotaCerta-Evento      ex.: SAIU_PARA_ENTREGA
 * X-RotaCerta-Entrega     id do aviso (o mesmo em todas as tentativas: use para não processar duas vezes)
 * X-RotaCerta-Tentativa   1, 2, 3...
 * X-RotaCerta-Timestamp   segundos desde 1970 (UTC)
 * X-RotaCerta-Assinatura  sha256=HMAC-SHA256(segredo, timestamp + "." + corpo) em hexadecimal
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DespachanteWebhook {

    private final FilaWebhookRepository fila;
    private final WebhookEntregaRepository entregaRepository;
    private final ValidadorUrlWebhook validadorUrl;
    private final HttpClient webhookHttpClient;
    private final TransactionTemplate tx;
    private final WebhookProperties props;
    private final Clock clock;

    private record Envio(Long id, String url, String segredo, String evento, String payload, int tentativa) {
    }

    private record Resultado(Integer statusHttp, String erro) {
        boolean sucesso() {
            return erro == null && statusHttp != null && statusHttp / 100 == 2;
        }
    }

    /** Uma rodada do despachante; devolve quantos avisos tentou enviar. */
    public int despacharPendentes() {
        Instant agora = clock.instant();
        List<Long> ids = tx.execute(s -> fila.reservar(agora, agora.plus(props.reserva()), props.loteDespacho()));
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        for (Long id : ids) {
            Envio envio = tx.execute(s -> entregaRepository.carregarComWebhook(id).map(e -> new Envio(e.getId(),
                    e.getWebhook().getUrl(), e.getWebhook().getSegredo(), e.getEvento().name(), e.getPayload(),
                    e.getTentativas() + 1)).orElse(null));
            if (envio == null) {
                continue; // webhook excluído no meio do caminho (cascade)
            }
            Resultado r = enviar(envio);
            tx.executeWithoutResult(s -> entregaRepository.findById(id).ifPresent(e -> registrar(e, r)));
        }
        return ids.size();
    }

    private void registrar(WebhookEntrega e, Resultado r) {
        Instant agora = clock.instant();
        if (r.sucesso()) {
            e.registrarSucesso(r.statusHttp(), agora);
            log.debug("Webhook {} entregue: aviso {} ({}) HTTP {}", e.getWebhook().getId(), e.getId(), e.getEvento(), r.statusHttp());
            return;
        }
        String erro = r.erro() != null ? r.erro() : "Resposta HTTP " + r.statusHttp() + " (esperado 2xx).";
        e.registrarFalha(r.statusHttp(), erro, agora, props.maxTentativas(), props.esperaInicial(), props.esperaMaxima());
        log.info("Webhook {}: aviso {} ({}) falhou na tentativa {}: {} -> {}", e.getWebhook().getId(), e.getId(),
                e.getEvento(), e.getTentativas(), erro, e.getStatus());
    }

    private Resultado enviar(Envio envio) {
        URI uri;
        try {
            uri = validadorUrl.validar(envio.url()); // o DNS pode ter mudado desde o cadastro
        } catch (RegraNegocioException e) {
            return new Resultado(null, e.getMessage());
        }
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(props.timeoutResposta())
                .header("Content-Type", "application/json")
                .header("User-Agent", "RotaCerta-Webhook/1.0")
                .header("X-RotaCerta-Evento", envio.evento())
                .header("X-RotaCerta-Entrega", String.valueOf(envio.id()))
                .header("X-RotaCerta-Tentativa", String.valueOf(envio.tentativa()))
                .header("X-RotaCerta-Timestamp", timestamp)
                .header("X-RotaCerta-Assinatura", AssinaturaWebhook.assinar(envio.segredo(), timestamp, envio.payload()))
                .POST(HttpRequest.BodyPublishers.ofString(envio.payload(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<Void> resposta = webhookHttpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return new Resultado(resposta.statusCode(), null);
        } catch (HttpTimeoutException e) {
            return new Resultado(null, "Sem resposta em " + props.timeoutResposta().toSeconds() + "s.");
        } catch (ConnectException e) {
            return new Resultado(null, "Não conectou em " + uri.getHost() + ":" + porta(uri) + " (conexão recusada ou fora do ar).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Resultado(null, "Envio interrompido.");
        } catch (Exception e) {
            return new Resultado(null, e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

    private static int porta(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
