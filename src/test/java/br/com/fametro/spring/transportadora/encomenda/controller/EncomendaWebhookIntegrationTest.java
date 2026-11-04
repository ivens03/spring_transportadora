package br.com.fametro.spring.transportadora.encomenda.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import br.com.fametro.spring.transportadora.webhook.service.AssinaturaWebhook;
import br.com.fametro.spring.transportadora.webhook.service.DespachanteWebhook;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal do Cliente + webhooks: o Administrativo cadastra o cliente, o Suporte vincula as cargas a ele,
 * a rota anda e o servidor do cliente (aqui um HttpServer local) recebe os avisos assinados.
 */
class EncomendaWebhookIntegrationTest extends IntegracaoTestBase {

    private static final String ENCOMENDAS = "/api/v1/encomendas";
    private static final String WEBHOOKS = "/api/v1/webhooks";
    private static final String LAT = "-3.731900";

    /** Aviso recebido pelo "servidor do cliente". */
    private record Recebido(String caminho, Map<String, List<String>> headers, String corpo) {
        String header(String nome) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(nome))
                    .map(e -> e.getValue().getFirst()).findFirst().orElse(null);
        }
    }

    private static HttpServer servidor;
    private static final List<Recebido> RECEBIDOS = new CopyOnWriteArrayList<>();

    @Autowired
    private DespachanteWebhook despachante;

    @BeforeAll
    static void subirReceptor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            RECEBIDOS.add(new Recebido(troca.getRequestURI().getPath(), troca.getRequestHeaders(), corpo));
            int status = troca.getRequestURI().getPath().startsWith("/erro") ? 500 : 204;
            troca.sendResponseHeaders(status, -1);
            troca.close();
        });
        servidor.start();
    }

    @AfterAll
    static void derrubarReceptor() {
        servidor.stop(0);
    }

    @BeforeEach
    void limpar() {
        RECEBIDOS.clear();
    }

    private static String url(String caminho) {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + caminho;
    }

    private record ClienteTeste(String matricula, String token) {
    }

    private ClienteTeste clienteAtivo() throws Exception {
        String email = "cliente-" + java.util.UUID.randomUUID() + "@loja.local";
        Criado c = criar(tokenAdmin(), """
                {"nome":"Loja Teste","email":"%s","vinculo":"CLIENTE"}""".formatted(email));
        assertThat(c.matricula()).matches("CLI-\\d{6}");
        ativar(c.matricula(), c.codigo(), SENHA).andExpect(status().isCreated());
        return new ClienteTeste(c.matricula(), tokenPainel(email, SENHA));
    }

    private static String cargaDo(String cliente, String bairro, String longitude) {
        String json = carga(bairro, LAT, longitude, "10").trim();
        return json.substring(0, json.length() - 1) + ",\"clienteMatricula\":\"" + cliente + "\"}";
    }

    private record Hook(long id, String segredo) {
    }

    private Hook webhook(String token, String caminho, String eventos) throws Exception {
        String body = enviar(post(WEBHOOKS), token, """
                {"url":"%s","eventos":%s}""".formatted(url(caminho), eventos))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/webhooks/\\d+$")))
                .andExpect(jsonPath("$.segredo").value(startsWith("whsec_")))
                .andExpect(jsonPath("$.webhook.segredoInicio").value(startsWith("whsec_")))
                .andReturn().getResponse().getContentAsString();
        return new Hook(((Number) JsonPath.read(body, "$.webhook.id")).longValue(), JsonPath.read(body, "$.segredo"));
    }

    private void despacharTudo() {
        while (despachante.despacharPendentes() > 0) {
            // até esvaziar o que está vencido
        }
    }

    private static String posicao(String lng, Instant quando) {
        return """
                {"latitude":%s,"longitude":%s,"registradaEm":"%s","velocidadeKmH":30,"precisaoM":8}"""
                .formatted(LAT, lng, quando.truncatedTo(ChronoUnit.MILLIS));
    }

    private String codigo(String suporte, long cargaId) throws Exception {
        return JsonPath.read(acessar(suporte, CARGAS + "/" + cargaId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.codigo");
    }

    @Test
    void clienteAcompanhaAsEncomendasERecebeOsWebhooks() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        ClienteTeste a = clienteAtivo();
        ClienteTeste b = clienteAtivo();
        Hook hookA = webhook(a.token(), "/loja-a", "null");                                     // todos os eventos
        Hook hookB = webhook(b.token(), "/loja-b", "[\"NAO_ENTREGUE\",\"AGUARDANDO_NOVA_ROTA\"]"); // só problemas

        // Suporte vê os clientes e vincula as cargas (perto e longe são de A, meio é de B)
        acessar(suporte, "/api/v1/clientes?status=ATIVO&size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].matricula", hasItem(a.matricula())));
        long perto = cadastrarCarga(suporte, cargaDo(a.matricula(), "Centro", "-38.516700"));
        long meio = cadastrarCarga(suporte, cargaDo(b.matricula().toLowerCase(), "Aldeota", "-38.496700"));
        long longe = cadastrarCarga(suporte, cargaDo(a.matricula(), "Papicu", "-38.476700"));
        acessar(suporte, CARGAS + "/" + perto)
                .andExpect(jsonPath("$.clienteMatricula").value(a.matricula()))
                .andExpect(jsonPath("$.clienteNome").value("Loja Teste"));
        String codPerto = codigo(suporte, perto);
        String codMeio = codigo(suporte, meio);
        String codLonge = codigo(suporte, longe);

        // rota perto → meio → longe
        long veiculo = cadastrarVeiculo(suporte, veiculo(placaUnica(), "DIESEL", "9"));
        EntregadorTeste ent = entregadorAtivo();
        Number rotaId = JsonPath.read(enviar(post(ROTAS), suporte, """
                {"veiculoId":%d,"entregadorMatricula":"%s","cargaIds":[%d,%d,%d]}"""
                .formatted(veiculo, ent.matricula(), longe, meio, perto))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        String rota = ROTAS + "/" + rotaId;

        // portal: cada cliente só vê as dele
        acessar(a.token(), ENCOMENDAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].codigo").value(codLonge)) // mais recente primeiro
                .andExpect(jsonPath("$.content[0].status").value("ROTEIRIZADA"));
        acessar(b.token(), ENCOMENDAS).andExpect(jsonPath("$.page.totalElements").value(1));
        acessar(a.token(), ENCOMENDAS + "/" + codMeio).andExpect(status().isNotFound());
        acessar(a.token(), ENCOMENDAS + "/" + codLonge.toLowerCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rastreio.rotaStatus").value("PLANEJADA"))
                .andExpect(jsonPath("$.rastreio.ordemParada").value(3))
                .andExpect(jsonPath("$.rastreio.entregasAntes").value(2))
                .andExpect(jsonPath("$.rastreio.entregador").value("Entregador"))     // só o primeiro nome
                .andExpect(jsonPath("$.rastreio.localizacao").value(nullValue()))
                .andExpect(jsonPath("$.rastreio.previsaoMinutos").value(greaterThan(10)))
                .andExpect(jsonPath("$.rastreio.previsaoChegadaEm").value(nullValue())); // ainda não saiu

        // sai para entrega e manda GPS (o 2º lote, logo em seguida, não gera novo aviso de localização)
        enviar(patch(rota), ent.token(), "{\"status\":\"EM_ANDAMENTO\"}").andExpect(status().isOk());
        enviar(post(rota + "/posicoes"), ent.token(), "{\"posicoes\":[%s]}"
                .formatted(posicao("-38.520000", Instant.now().minusSeconds(5)))).andExpect(status().isCreated());
        enviar(post(rota + "/posicoes"), ent.token(), "{\"posicoes\":[%s]}"
                .formatted(posicao("-38.519000", Instant.now().minusSeconds(1)))).andExpect(status().isCreated());
        acessar(a.token(), ENCOMENDAS + "/" + codLonge)
                .andExpect(jsonPath("$.encomenda.status").value("EM_TRANSITO"))
                .andExpect(jsonPath("$.rastreio.rotaStatus").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.rastreio.localizacao.longitude").value(-38.519))
                .andExpect(jsonPath("$.rastreio.previsaoChegadaEm").value(notNullValue()));

        // entregas: perto ok, meio não, longe ok → rota conclui e "meio" volta para a fila
        enviar(patch(rota + "/paradas/1"), ent.token(), "{\"status\":\"ENTREGUE\"}").andExpect(status().isOk());
        enviar(patch(rota + "/paradas/2"), ent.token(), "{\"status\":\"NAO_ENTREGUE\",\"motivo\":\"Destinatário ausente\"}")
                .andExpect(status().isOk());
        enviar(patch(rota + "/paradas/3"), ent.token(), "{\"status\":\"ENTREGUE\"}")
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        acessar(a.token(), ENCOMENDAS + "/" + codLonge)
                .andExpect(jsonPath("$.encomenda.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.rastreio.entregueEm").value(notNullValue()))
                .andExpect(jsonPath("$.rastreio.localizacao").value(nullValue()))
                .andExpect(jsonPath("$.eventos[*].tipo").value(org.hamcrest.Matchers.contains(
                        "CADASTRADA", "ROTEIRIZADA", "SAIU_PARA_ENTREGA", "ENTREGUE")));
        acessar(b.token(), ENCOMENDAS + "/" + codMeio)
                .andExpect(jsonPath("$.encomenda.status").value("AGUARDANDO_ROTA"))
                .andExpect(jsonPath("$.rastreio").value(nullValue()))
                .andExpect(jsonPath("$.eventos[3].detalhe").value("Destinatário ausente"))
                .andExpect(jsonPath("$.eventos[4].tipo").value("AGUARDANDO_NOVA_ROTA"));

        // o despachante entrega tudo
        despacharTudo();
        List<Recebido> lojaA = RECEBIDOS.stream().filter(r -> r.caminho().equals("/loja-a")).toList();
        List<Recebido> lojaB = RECEBIDOS.stream().filter(r -> r.caminho().equals("/loja-b")).toList();
        assertThat(lojaA).extracting(r -> r.header("X-RotaCerta-Evento")).containsExactlyInAnyOrder(
                "CADASTRADA", "CADASTRADA", "ROTEIRIZADA", "ROTEIRIZADA", "SAIU_PARA_ENTREGA", "SAIU_PARA_ENTREGA",
                "LOCALIZACAO", "LOCALIZACAO", "ENTREGUE", "ENTREGUE");
        assertThat(lojaB).extracting(r -> r.header("X-RotaCerta-Evento"))
                .containsExactlyInAnyOrder("NAO_ENTREGUE", "AGUARDANDO_NOVA_ROTA");

        // assinatura confere com o segredo de cada cliente
        for (Recebido r : lojaA) {
            assertThat(r.header("X-RotaCerta-Assinatura"))
                    .isEqualTo(AssinaturaWebhook.assinar(hookA.segredo(), r.header("X-RotaCerta-Timestamp"), r.corpo()));
            assertThat(r.header("X-RotaCerta-Entrega")).isNotBlank();
            assertThat(r.header("X-RotaCerta-Tentativa")).isEqualTo("1");
        }
        for (Recebido r : lojaB) {
            assertThat(r.header("X-RotaCerta-Assinatura"))
                    .isEqualTo(AssinaturaWebhook.assinar(hookB.segredo(), r.header("X-RotaCerta-Timestamp"), r.corpo()));
        }

        // o aviso de localização diz onde o veículo está e quando chega
        Recebido loc = lojaA.stream().filter(r -> r.header("X-RotaCerta-Evento").equals("LOCALIZACAO")
                && JsonPath.read(r.corpo(), "$.encomenda.codigo").equals(codLonge)).findFirst().orElseThrow();
        assertThat((Object) JsonPath.read(loc.corpo(), "$.rastreio.localizacao.latitude")).isNotNull();
        assertThat(((Number) JsonPath.read(loc.corpo(), "$.rastreio.previsaoMinutos")).intValue()).isPositive();
        assertThat((Integer) JsonPath.read(loc.corpo(), "$.rastreio.entregasAntes")).isEqualTo(2);
        Recebido falha = lojaB.stream().filter(r -> r.header("X-RotaCerta-Evento").equals("NAO_ENTREGUE")).findFirst().orElseThrow();
        assertThat((String) JsonPath.read(falha.corpo(), "$.detalhe")).isEqualTo("Destinatário ausente");
        assertThat((String) JsonPath.read(falha.corpo(), "$.encomenda.codigo")).isEqualTo(codMeio);

        // histórico de avisos do webhook
        acessar(a.token(), WEBHOOKS + "/" + hookA.id() + "/entregas?size=50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(10))
                .andExpect(jsonPath("$.content[*].status", not(hasItem("PENDENTE"))))
                .andExpect(jsonPath("$.content[0].ultimoStatusHttp").value(204));
        acessar(b.token(), WEBHOOKS + "/" + hookA.id() + "/entregas").andExpect(status().isNotFound());
        assertThat(codPerto).isNotBlank();
    }

    @Test
    void gestaoDoWebhookERetentativa() throws Exception {
        ClienteTeste c = clienteAtivo();
        ClienteTeste outro = clienteAtivo();

        // validações
        enviar(post(WEBHOOKS), c.token(), "{\"url\":\"ftp://127.0.0.1/x\"}").andExpect(status().isUnprocessableContent());
        enviar(post(WEBHOOKS), c.token(), "{\"url\":\"%s\",\"eventos\":[\"PING\"]}".formatted(url("/x")))
                .andExpect(status().isUnprocessableContent());
        enviar(post(WEBHOOKS), c.token(), "{\"url\":\"\"}").andExpect(status().isBadRequest());
        Hook hook = webhook(c.token(), "/erro", "[]");
        enviar(post(WEBHOOKS), c.token(), "{\"url\":\"%s\"}".formatted(url("/erro"))).andExpect(status().isConflict());
        acessar(c.token(), WEBHOOKS + "/" + hook.id())
                .andExpect(jsonPath("$.eventos.length()").value(8)) // vazio = todos os assináveis
                .andExpect(jsonPath("$.segredo").doesNotExist());
        acessar(outro.token(), WEBHOOKS + "/" + hook.id()).andExpect(status().isNotFound());

        // PING num servidor que responde 500: fica PENDENTE para tentar de novo
        enviar(post(WEBHOOKS + "/" + hook.id() + "/pings"), c.token(), "")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.evento").value("PING"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
        despachante.despacharPendentes();
        assertThat(RECEBIDOS).anyMatch(r -> r.caminho().equals("/erro") && "PING".equals(r.header("X-RotaCerta-Evento")));
        acessar(c.token(), WEBHOOKS + "/" + hook.id() + "/entregas")
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.content[0].tentativas").value(1))
                .andExpect(jsonPath("$.content[0].ultimoStatusHttp").value(500))
                .andExpect(jsonPath("$.content[0].proximaTentativaEm").value(notNullValue()))
                .andExpect(jsonPath("$.content[0].payload.evento").value("PING"));

        // troca de segredo, pausa, exclusão
        String novo = JsonPath.read(enviar(post(WEBHOOKS + "/" + hook.id() + "/segredos"), c.token(), "")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.segredo");
        assertThat(novo).startsWith("whsec_").isNotEqualTo(hook.segredo());
        enviar(patch(WEBHOOKS + "/" + hook.id()), c.token(), "{}").andExpect(status().isUnprocessableContent());
        enviar(patch(WEBHOOKS + "/" + hook.id()), c.token(), "{\"ativo\":false,\"eventos\":[\"ENTREGUE\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.eventos[0]").value("ENTREGUE"));
        enviar(post(WEBHOOKS + "/" + hook.id() + "/pings"), c.token(), "").andExpect(status().isUnprocessableContent());
        mvc.perform(delete(WEBHOOKS + "/" + hook.id()).header("Authorization", "Bearer " + outro.token()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(WEBHOOKS + "/" + hook.id()).header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isNoContent());
        acessar(c.token(), WEBHOOKS + "/" + hook.id()).andExpect(status().isNotFound());
    }

    @Test
    void cadastroDoClienteEPermissoes() throws Exception {
        String admin = tokenAdmin();
        criarResultado(admin, "{\"nome\":\"Sem Email\",\"vinculo\":\"CLIENTE\"}").andExpect(status().isUnprocessableContent());
        criarResultado(admin, """
                {"nome":"Com Depto","email":"%s","vinculo":"CLIENTE","departamento":"SUPORTE"}""".formatted(emailUnico()))
                .andExpect(status().isUnprocessableContent());

        ClienteTeste c = clienteAtivo();
        acessar(admin, USUARIOS + "/" + c.matricula())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.vinculo").value("CLIENTE"))
                .andExpect(jsonPath("$.modulo").value("Portal do Cliente"));
        acessar(c.token(), "/api/v1/auth/sessao")
                .andExpect(jsonPath("$.permissoes", hasItem("encomenda:acompanhar")))
                .andExpect(jsonPath("$.permissoes", hasItem("webhook:gerenciar")));

        // cliente não entra no que é interno
        for (String url : new String[]{CARGAS, ROTAS, USUARIOS, VEICULOS, "/api/v1/clientes", "/api/v1/entregadores"}) {
            acessar(c.token(), url).andExpect(status().isForbidden());
        }
        // e funcionário/entregador não entram no portal
        String suporte = tokenFuncionario("SUPORTE");
        acessar(suporte, ENCOMENDAS).andExpect(status().isForbidden());
        acessar(suporte, WEBHOOKS).andExpect(status().isForbidden());
        acessar(entregadorAtivo().token(), ENCOMENDAS).andExpect(status().isForbidden());

        // carga só aceita cliente de verdade
        enviar(post(CARGAS), suporte, cargaDo("CLI-999999", "Centro", "-38.5")).andExpect(status().isNotFound());
        enviar(post(CARGAS), suporte, cargaDo(entregadorAtivo().matricula(), "Centro", "-38.5"))
                .andExpect(status().isUnprocessableContent());

        // troca de dono pelo PATCH: o novo cliente passa a ver; "" desvincula
        long carga = cadastrarCarga(suporte, carga("Centro", LAT, "-38.5", "1"));
        String cod = codigo(suporte, carga);
        acessar(c.token(), ENCOMENDAS + "/" + cod).andExpect(status().isNotFound());
        enviar(patch(CARGAS + "/" + carga), suporte, "{\"clienteMatricula\":\"%s\"}".formatted(c.matricula()))
                .andExpect(jsonPath("$.clienteMatricula").value(c.matricula()));
        acessar(c.token(), ENCOMENDAS + "/" + cod)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[0].tipo").value("CADASTRADA"));
        enviar(patch(CARGAS + "/" + carga), suporte, "{\"clienteMatricula\":\"\"}")
                .andExpect(jsonPath("$.clienteMatricula").value(nullValue()));
        acessar(c.token(), ENCOMENDAS + "/" + cod).andExpect(status().isNotFound());
    }
}
