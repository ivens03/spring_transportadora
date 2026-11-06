package br.com.fametro.spring.transportadora.rota.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo da rota: Suporte escolhe as cargas → sistema gera a rota otimizada → entregador inicia no app,
 * envia GPS e baixa as paradas → rota conclui sozinha.
 */
class RotaControllerIntegrationTest extends IntegracaoTestBase {

    /** Cargas em linha a leste do depósito (-3.7319, -38.5267): perto, meio e longe. */
    private static final String LAT = "-3.731900";

    private record Cenario(String suporte, long veiculo, EntregadorTeste entregador, long perto, long meio, long longe) {
    }

    private Cenario cenario() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        long veiculo = cadastrarVeiculo(suporte, veiculo(placaUnica(), "DIESEL", "9"));
        // cadastradas fora de ordem de propósito
        long longe = cadastrarCarga(suporte, carga("Papicu", LAT, "-38.476700", "100"));
        long perto = cadastrarCarga(suporte, carga("Centro", LAT, "-38.516700", "100"));
        long meio = cadastrarCarga(suporte, carga("Aldeota", LAT, "-38.496700", "100"));
        return new Cenario(suporte, veiculo, entregadorAtivo(), perto, meio, longe);
    }

    private ResultActions gerar(String token, long veiculo, String matricula, long... cargas) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (long c : cargas) ids.append(ids.isEmpty() ? "" : ",").append(c);
        return enviar(post(ROTAS), token, """
                {"veiculoId":%d,"entregadorMatricula":"%s","cargaIds":[%s]}""".formatted(veiculo, matricula, ids));
    }

    private long gerarRota(Cenario c) throws Exception {
        Number id = JsonPath.read(gerar(c.suporte(), c.veiculo(), c.entregador().matricula(), c.longe(), c.perto(), c.meio())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private ResultActions statusRota(String token, long rota, String status) throws Exception {
        return enviar(patch(ROTAS + "/" + rota), token, """
                {"status":"%s"}""".formatted(status));
    }

    private ResultActions baixar(String token, long rota, int ordem, String json) throws Exception {
        return enviar(patch(ROTAS + "/" + rota + "/paradas/" + ordem), token, json);
    }

    private static String posicao(String lng, Instant quando) {
        return """
                {"latitude":%s,"longitude":%s,"registradaEm":"%s","velocidadeKmH":30.5,"precisaoM":8}"""
                .formatted(LAT, lng, quando.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void fluxoCompletoDaRota() throws Exception {
        Cenario c = cenario();
        String app = c.entregador().token();

        // 1) Suporte gera: ordem otimizada perto → meio → longe, volta ao depósito
        String body = gerar(c.suporte(), c.veiculo(), c.entregador().matricula().toLowerCase(), c.longe(), c.perto(), c.meio())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/rotas/\\d+$")))
                .andExpect(jsonPath("$.codigo").value(matchesPattern("ROT-\\d{6}")))
                .andExpect(jsonPath("$.status").value("PLANEJADA"))
                .andExpect(jsonPath("$.paradas[0].cargaId").value(c.perto()))
                .andExpect(jsonPath("$.paradas[1].cargaId").value(c.meio()))
                .andExpect(jsonPath("$.paradas[2].cargaId").value(c.longe()))
                .andExpect(jsonPath("$.paradas[0].ordem").value(1))
                .andExpect(jsonPath("$.trajeto.length()").value(5))          // origem + 3 + retorno
                .andExpect(jsonPath("$.trajeto[0].tipo").value("ORIGEM"))
                .andExpect(jsonPath("$.trajeto[4].tipo").value("RETORNO"))
                .andExpect(jsonPath("$.pesoTotalKg").value(300.0))
                .andExpect(jsonPath("$.distanciaEstimadaKm").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.duracaoEstimadaMin").value(greaterThan(15)))  // inclui 3 × 5 min por parada
                .andExpect(jsonPath("$.proximaParada").value(1))
                .andExpect(jsonPath("$.ultimaPosicao").doesNotExist())
                .andExpect(jsonPath("$.progresso.pendentes").value(3))
                .andReturn().getResponse().getContentAsString();
        long rota = ((Number) JsonPath.read(body, "$.id")).longValue();
        acessar(c.suporte(), CARGAS + "/" + c.perto()).andExpect(jsonPath("$.status").value("ROTEIRIZADA"));

        // 2) o entregador vê só as dele; outro entregador não enxerga (404)
        acessar(app, ROTAS + "?status=PLANEJADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].totalParadas").value(3));
        EntregadorTeste outro = entregadorAtivo();
        acessar(outro.token(), ROTAS + "/" + rota).andExpect(status().isNotFound());
        acessar(outro.token(), ROTAS).andExpect(jsonPath("$.content.length()").value(0));

        // 3) antes de iniciar não aceita GPS; o Suporte não inicia rota
        enviar(post(ROTAS + "/" + rota + "/posicoes"), app, """
                {"posicoes":[%s]}""".formatted(posicao("-38.52", Instant.now()))).andExpect(status().isUnprocessableContent());
        statusRota(c.suporte(), rota, "EM_ANDAMENTO").andExpect(status().isUnprocessableContent());

        // 4) entregador inicia: a resposta já é a tela da rota
        statusRota(app, rota, "EM_ANDAMENTO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.iniciadaEm").value(notNullValue()))
                .andExpect(jsonPath("$.proximaParada").value(1))
                .andExpect(jsonPath("$.distanciaAteProximaParadaKm").value(greaterThan(0.0)));
        statusRota(app, rota, "EM_ANDAMENTO").andExpect(status().isOk());                 // reenvio
        statusRota(app, rota, "CANCELADA").andExpect(status().isUnprocessableContent());  // entregador não cancela
        statusRota(c.suporte(), rota, "CANCELADA").andExpect(status().isUnprocessableContent()); // já saiu
        acessar(c.suporte(), CARGAS + "/" + c.perto()).andExpect(jsonPath("$.status").value("EM_TRANSITO"));

        // 5) GPS em lote, com reenvio (offline-first)
        Instant t1 = Instant.now().minusSeconds(120);
        Instant t2 = Instant.now().minusSeconds(60);
        String lote = """
                {"posicoes":[%s,%s,%s]}""".formatted(posicao("-38.5250", t1), posicao("-38.5200", t2), posicao("-38.5200", t2));
        enviar(post(ROTAS + "/" + rota + "/posicoes"), app, lote)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recebidas").value(3))
                .andExpect(jsonPath("$.gravadas").value(2))
                .andExpect(jsonPath("$.duplicadas").value(1));
        enviar(post(ROTAS + "/" + rota + "/posicoes"), app, lote)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gravadas").value(0));
        enviar(post(ROTAS + "/" + rota + "/posicoes"), app, """
                {"posicoes":[%s]}""".formatted(posicao("-38.52", Instant.now().plusSeconds(3600))))
                .andExpect(status().isUnprocessableContent()); // relógio adiantado
        enviar(post(ROTAS + "/" + rota + "/posicoes"), c.suporte(), lote).andExpect(status().isForbidden());

        // 6) Suporte acompanha no mapa (rastro incremental com "desde")
        acessar(c.suporte(), ROTAS + "/" + rota)
                .andExpect(jsonPath("$.ultimaPosicao.longitude").value(-38.52))
                .andExpect(jsonPath("$.minutosAteProximaParada").value(greaterThan(0)));
        acessar(c.suporte(), ROTAS + "/" + rota + "/posicoes").andExpect(jsonPath("$.length()").value(2));
        acessar(c.suporte(), ROTAS + "/" + rota + "/posicoes?desde=" + t1.truncatedTo(ChronoUnit.MILLIS))
                .andExpect(jsonPath("$.length()").value(1));

        // 7) baixa das paradas
        baixar(app, rota, 1, """
                {"status":"ENTREGUE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas[0].status").value("ENTREGUE"))
                .andExpect(jsonPath("$.proximaParada").value(2))
                .andExpect(jsonPath("$.progresso.percentual").value(33));
        baixar(app, rota, 1, """
                {"status":"ENTREGUE"}""").andExpect(status().isOk());                  // reenvio
        baixar(app, rota, 1, """
                {"status":"NAO_ENTREGUE","motivo":"x"}""").andExpect(status().isConflict());
        baixar(app, rota, 2, """
                {"status":"NAO_ENTREGUE"}""").andExpect(status().isUnprocessableContent()); // sem motivo
        baixar(app, rota, 9, """
                {"status":"ENTREGUE"}""").andExpect(status().isNotFound());
        baixar(app, rota, 2, """
                {"status":"NAO_ENTREGUE","motivo":"Destinatário ausente"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaParada").value(3));
        baixar(app, rota, 3, """
                {"status":"ENTREGUE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").value(notNullValue()))
                .andExpect(jsonPath("$.proximaParada").doesNotExist())
                .andExpect(jsonPath("$.progresso.entregues").value(2))
                .andExpect(jsonPath("$.progresso.naoEntregues").value(1));

        // 8) entregues ficam ENTREGUE; a não entregue volta para a fila contando a tentativa
        acessar(c.suporte(), CARGAS + "/" + c.perto()).andExpect(jsonPath("$.status").value("ENTREGUE"));
        acessar(c.suporte(), CARGAS + "/" + c.meio())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ROTA"))
                .andExpect(jsonPath("$.tentativasEntrega").value(1));

        // 9) GPS atrasado ainda entra depois de concluir; veículo e entregador ficam livres para outra rota
        enviar(post(ROTAS + "/" + rota + "/posicoes"), app, """
                {"posicoes":[%s]}""".formatted(posicao("-38.50", Instant.now().minusSeconds(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gravadas").value(1));
        baixar(app, rota, 1, """
                {"status":"ENTREGUE"}""").andExpect(status().isOk()); // reenvio tardio continua inofensivo
        gerar(c.suporte(), c.veiculo(), c.entregador().matricula(), c.meio()).andExpect(status().isCreated());
    }

    @Test
    void validacoesAoGerar() throws Exception {
        Cenario c = cenario();
        String ent = c.entregador().matricula();

        long pesada = cadastrarCarga(c.suporte(), carga("Centro", LAT, "-38.51", "700"));
        gerar(c.suporte(), c.veiculo(), ent, pesada)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("capacidade")));
        gerar(c.suporte(), c.veiculo(), ent, c.perto(), c.perto()).andExpect(status().isUnprocessableContent());
        gerar(c.suporte(), c.veiculo(), ent, 999999).andExpect(status().isNotFound());
        gerar(c.suporte(), 999999, ent, c.perto()).andExpect(status().isNotFound());
        gerar(c.suporte(), c.veiculo(), "ENT-999999", c.perto()).andExpect(status().isNotFound());
        gerar(c.suporte(), c.veiculo(), "FUN-TESTE-NAO", c.perto()).andExpect(status().isNotFound());
        enviar(post(ROTAS), c.suporte(), """
                {"veiculoId":%d,"entregadorMatricula":"%s","cargaIds":[%d],"origemLatitude":-3.7}"""
                .formatted(c.veiculo(), ent, c.perto())).andExpect(status().isUnprocessableContent());
        enviar(post(ROTAS), c.suporte(), """
                {"veiculoId":%d,"entregadorMatricula":"%s","cargaIds":[]}""".formatted(c.veiculo(), ent))
                .andExpect(status().isBadRequest());

        // funcionário não é entregador
        String emailSup = emailUnico();
        Criado sup = criar(tokenAdmin(), funcionario(emailSup, "SUPORTE"));
        gerar(c.suporte(), c.veiculo(), sup.matricula(), c.perto()).andExpect(status().isUnprocessableContent());

        long rota = gerarRota(c);
        // carga já roteirizada, veículo e entregador ocupados
        long outraCarga = cadastrarCarga(c.suporte(), carga("Centro", LAT, "-38.51", "1"));
        long outroVeiculo = cadastrarVeiculo(c.suporte(), veiculo(placaUnica(), "DIESEL", "9"));
        EntregadorTeste outroEnt = entregadorAtivo();
        gerar(c.suporte(), outroVeiculo, outroEnt.matricula(), c.perto()).andExpect(status().isUnprocessableContent());
        gerar(c.suporte(), c.veiculo(), outroEnt.matricula(), outraCarga).andExpect(status().isConflict());
        gerar(c.suporte(), outroVeiculo, ent, outraCarga).andExpect(status().isConflict());

        // veículo inativo
        enviar(patch(VEICULOS + "/" + outroVeiculo), c.suporte(), """
                {"status":"EM_MANUTENCAO"}""").andExpect(status().isOk());
        gerar(c.suporte(), outroVeiculo, outroEnt.matricula(), outraCarga).andExpect(status().isUnprocessableContent());

        // cancelar antes de sair devolve as cargas para a fila (sem contar tentativa)
        statusRota(c.suporte(), rota, "CANCELADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.canceladaEm").value(notNullValue()));
        acessar(c.suporte(), CARGAS + "/" + c.perto())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ROTA"))
                .andExpect(jsonPath("$.tentativasEntrega").value(0));
        statusRota(c.suporte(), rota, "CONCLUIDA").andExpect(status().isUnprocessableContent());
        baixar(c.entregador().token(), rota, 1, """
                {"status":"ENTREGUE"}""").andExpect(status().isUnprocessableContent()); // rota cancelada
    }

    @Test
    void origemInformadaSemRetorno() throws Exception {
        Cenario c = cenario();
        // saindo do ponto mais a leste, a ordem se inverte: longe → meio → perto
        enviar(post(ROTAS), c.suporte(), """
                {"veiculoId":%d,"entregadorMatricula":"%s","cargaIds":[%d,%d,%d],
                 "origemLatitude":-3.7319,"origemLongitude":-38.4667,"retornarAOrigem":false}"""
                .formatted(c.veiculo(), c.entregador().matricula(), c.perto(), c.meio(), c.longe()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.retornaOrigem").value(false))
                .andExpect(jsonPath("$.trajeto.length()").value(4))
                .andExpect(jsonPath("$.paradas[0].cargaId").value(c.longe()))
                .andExpect(jsonPath("$.paradas[2].cargaId").value(c.perto()));
    }

    @Test
    void quemPodeOQue() throws Exception {
        Cenario c = cenario();
        long rota = gerarRota(c);
        String app = c.entregador().token();

        gerar(app, c.veiculo(), c.entregador().matricula(), c.perto()).andExpect(status().isForbidden());
        acessar(app, CARGAS).andExpect(status().isForbidden());
        for (String dep : new String[]{"ADMINISTRATIVO", "FINANCEIRO", "DESENVOLVIMENTO"}) {
            String token = tokenFuncionario(dep);
            acessar(token, ROTAS + "/" + rota).andExpect(status().isForbidden());
            statusRota(token, rota, "CANCELADA").andExpect(status().isForbidden());
        }
        baixar(c.suporte(), rota, 1, """
                {"status":"ENTREGUE"}""").andExpect(status().isForbidden()); // baixa é só pelo app
    }

    private ResultActions incluir(String token, long rota, long... cargas) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (long c : cargas) ids.append(ids.isEmpty() ? "" : ",").append(c);
        return enviar(post(ROTAS + "/" + rota + "/cargas"), token, """
                {"cargaIds":[%s]}""".formatted(ids));
    }

    @Test
    void incluirEntregaNaRotaQueAindaNaoSaiu() throws Exception {
        Cenario c = cenario();
        Number id = JsonPath.read(gerar(c.suporte(), c.veiculo(), c.entregador().matricula(), c.longe(), c.perto())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        long rota = id.longValue();

        // a do meio entra entre as duas: a ordem é recalculada para todas
        incluir(c.suporte(), rota, c.meio())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANEJADA"))
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[0].cargaId").value(c.perto()))
                .andExpect(jsonPath("$.paradas[1].cargaId").value(c.meio()))
                .andExpect(jsonPath("$.paradas[1].ordem").value(2))
                .andExpect(jsonPath("$.paradas[2].cargaId").value(c.longe()))
                .andExpect(jsonPath("$.pesoTotalKg").value(300.0));
        acessar(c.suporte(), CARGAS + "/" + c.meio()).andExpect(jsonPath("$.status").value("ROTEIRIZADA"));

        // carga que já está em rota, capacidade e quem pode
        incluir(c.suporte(), rota, c.meio()).andExpect(status().isUnprocessableEntity());
        long pesada = cadastrarCarga(c.suporte(), carga("Meireles", LAT, "-38.506700", "400"));
        incluir(c.suporte(), rota, pesada).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("capacidade")));
        incluir(c.entregador().token(), rota, pesada).andExpect(status().isForbidden());
        incluir(c.suporte(), 999_999, pesada).andExpect(status().isNotFound());

        // depois que saiu, não entra mais nada
        statusRota(c.entregador().token(), rota, "EM_ANDAMENTO").andExpect(status().isOk());
        long leve = cadastrarCarga(c.suporte(), carga("Benfica", LAT, "-38.526700", "5"));
        incluir(c.suporte(), rota, leve).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("antes de ela sair")));
    }
}
