package br.com.fametro.spring.transportadora;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação inteira com Postgres em container (Testcontainers)
 * e oferece atalhos para cadastrar, ativar e logar usuários.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTestBase {

    protected static final String USUARIOS = "/api/v1/usuarios";
    protected static final String VEICULOS = "/api/v1/veiculos";
    protected static final String METRICAS = "/api/v1/metricas-consumo";
    protected static final String PIN = "482917";
    protected static final String SENHA = "SenhaForte2026";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Simula que passaram 31 dias desde o cadastro da chave de salt ativa do usuário. */
    protected void vencerChaveSalt(String matricula) {
        jdbc.update("""
                update seguranca.chave_salt
                   set cadastrada_em = now() - interval '31 days', expira_em = now() - interval '1 day'
                 where ativa and usuario_id = (select id from usuario where matricula = ?)""", matricula);
    }

    protected record Criado(String matricula, String codigo) {
    }

    protected String tokenAdmin() throws Exception {
        return JsonPath.read(loginPainel("admin@teste.local", "SenhaAdmin123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    protected String tokenPainel(String email, String senha) throws Exception {
        return JsonPath.read(loginPainel(email, senha)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    protected Criado criar(String token, String json) throws Exception {
        String body = criarResultado(token, json)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return new Criado(JsonPath.read(body, "$.matricula"), JsonPath.read(body, "$.codigoAtivacao"));
    }

    protected ResultActions criarResultado(String token, String json) throws Exception {
        return mvc.perform(post(USUARIOS).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Primeiro acesso: POST /api/v1/auth/credenciais (201). */
    protected ResultActions ativar(String matricula, String codigo, String credencial) throws Exception {
        return mvc.perform(post("/api/v1/auth/credenciais").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","codigoAtivacao":"%s","credencial":"%s"}""".formatted(matricula, codigo, credencial)));
    }

    /** Troca de PIN/senha: PATCH /api/v1/auth/credenciais/{matricula} (200). */
    protected ResultActions trocar(String matricula, String atual, String nova) throws Exception {
        return mvc.perform(patch("/api/v1/auth/credenciais/{m}", matricula).contentType(MediaType.APPLICATION_JSON).content("""
                {"credencialAtual":"%s","novaCredencial":"%s"}""".formatted(atual, nova)));
    }

    /** Cria funcionário do departamento, ativa a senha e devolve o token do painel. */
    protected String tokenFuncionario(String departamento) throws Exception {
        String email = emailUnico();
        Criado fun = criar(tokenAdmin(), funcionario(email, departamento));
        ativar(fun.matricula(), fun.codigo(), SENHA).andExpect(status().isCreated());
        return tokenPainel(email, SENHA);
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions loginApp(String matricula, String pin) throws Exception {
        return mvc.perform(post("/api/v1/auth/app/tokens").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","pin":"%s"}""".formatted(matricula, pin)));
    }

    protected ResultActions loginPainel(String email, String senha) throws Exception {
        return mvc.perform(post("/api/v1/auth/painel/tokens").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","senha":"%s"}""".formatted(email, senha)));
    }

    protected ResultActions acessar(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected static String entregador() {
        return """
                {"nome":"Entregador Teste","vinculo":"EXTERNO"}""";
    }

    protected static String funcionario(String email, String departamento) {
        return """
                {"nome":"Funcionario Teste","email":"%s","vinculo":"INTERNO","departamento":"%s"}"""
                .formatted(email, departamento);
    }

    /** Placa Mercosul aleatória (ABC1D23), para os testes não colidirem no índice único. */
    protected static String placaUnica() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) sb.append((char) ('A' + r.nextInt(26)));
        return sb.append(r.nextInt(10)).append((char) ('A' + r.nextInt(26))).append(r.nextInt(10)).append(r.nextInt(10)).toString();
    }

    /** JSON de cadastro de veículo. */
    protected static String veiculo(String placa, String combustivel, String consumoReferencia) {
        return """
                {"placa":"%s","marca":"Fiat","modelo":"Fiorino","anoFabricacao":2023,"tipo":"UTILITARIO",
                 "combustivel":"%s","capacidadeCargaKg":650,"capacidadeVolumeM3":3.1,
                 "consumoReferenciaKmPorLitro":%s,"quilometragemAtual":100}"""
                .formatted(placa, combustivel, consumoReferencia);
    }

    /** Cadastra veículo e devolve o id. */
    protected long cadastrarVeiculo(String token, String json) throws Exception {
        Number id = JsonPath.read(enviar(post(VEICULOS), token, json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    protected ResultActions abastecer(String token, long veiculoId, String dataHora, int odometro,
                                      String litros, String valor, String combustivel) throws Exception {
        return enviar(post(VEICULOS + "/{id}/abastecimentos", veiculoId), token, """
                {"dataHora":"%s","odometroKm":%d,"litros":%s,"valorTotal":%s,"combustivel":"%s","posto":"Posto Teste"}"""
                .formatted(dataHora, odometro, litros, valor, combustivel));
    }

    protected static String emailUnico() {
        return "func-" + UUID.randomUUID() + "@rotacerta.local";
    }
}
