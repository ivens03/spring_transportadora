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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    protected static final String USUARIOS = "/api/v1/painel/gestao/usuarios";
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

    protected ResultActions ativar(String matricula, String codigo, String credencial) throws Exception {
        return mvc.perform(post("/api/v1/auth/ativacoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","codigoAtivacao":"%s","novaCredencial":"%s"}""".formatted(matricula, codigo, credencial)));
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

    protected static String emailUnico() {
        return "func-" + UUID.randomUUID() + "@rotacerta.local";
    }
}
