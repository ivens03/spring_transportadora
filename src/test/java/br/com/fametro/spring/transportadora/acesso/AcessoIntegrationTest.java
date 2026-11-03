package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import br.com.fametro.spring.transportadora.acesso.dominio.Departamento;

import static br.com.fametro.spring.transportadora.acesso.dominio.Departamento.ADMINISTRATIVO;
import static br.com.fametro.spring.transportadora.acesso.dominio.Departamento.DESENVOLVIMENTO;
import static br.com.fametro.spring.transportadora.acesso.dominio.Departamento.FINANCEIRO;
import static br.com.fametro.spring.transportadora.acesso.dominio.Departamento.SUPORTE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo de acesso ponta a ponta: matrícula -> ativação -> login por tipo de credencial -> módulo permitido. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AcessoIntegrationTest {

    private static final String PIN = "482917";
    private static final String SENHA = "SenhaForte2026";

    /** Rota "sonda" de cada módulo do painel: se o perfil pode entrar, cai em 404 (ainda não existe); se não, 403. */
    private static final Map<Departamento, String> MODULO = Map.of(
            ADMINISTRATIVO, "/api/painel/gestao/sonda",
            SUPORTE, "/api/painel/operacao/sonda",
            FINANCEIRO, "/api/painel/financeiro/sonda",
            DESENVOLVIMENTO, "/api/painel/monitoramento/sonda");

    @Autowired
    MockMvc mvc;

    // ------------------------------------------------------------------ entregador

    @Test
    void entregadorAtivaComPinEntraNoAppENaoEntraNoPainel() throws Exception {
        String admin = tokenAdmin();
        Criada ent = criar(admin, entregador());
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isNoContent());

        String token = loginApp(ent.matricula(), PIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoCredencial").value("APP_ENTREGADOR"))
                .andExpect(jsonPath("$.expiresIn").value(12 * 3600))
                .andReturn().getResponse().getContentAsString();
        String bearer = JsonPath.read(token, "$.accessToken");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ENTREGADOR"))
                .andExpect(jsonPath("$.modulo").value("App do Entregador"))
                .andExpect(jsonPath("$.permissoes.length()").value(3));

        acessar(bearer, "/api/app/sonda").andExpect(status().isNotFound()); // autorizado
        for (String modulo : MODULO.values()) {
            acessar(bearer, modulo).andExpect(status().isForbidden()); // sem acesso a painel web
        }
        acessar(bearer, "/api/painel/gestao/matriculas").andExpect(status().isForbidden());
    }

    @Test
    void entregadorNaoUsaLoginDoPainelMesmoComEmail() throws Exception {
        String email = emailUnico();
        Criada ent = criar(tokenAdmin(), """
                {"nome":"Entregador Com Email","email":"%s","vinculo":"EXTERNO"}""".formatted(email));
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isNoContent());

        loginPainel(email, PIN).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ funcionários

    @ParameterizedTest
    @EnumSource(Departamento.class)
    void funcionarioEntraSoNoModuloDoSeuDepartamento(Departamento dep)
            throws Exception {
        String email = emailUnico();
        Criada fun = criar(tokenAdmin(), funcionario(email, dep.name()));
        ativar(fun.matricula(), fun.codigo(), SENHA).andExpect(status().isNoContent());

        String bearer = JsonPath.read(loginPainel(email, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value(dep.name()))
                .andExpect(jsonPath("$.tipoCredencial").value("PAINEL_WEB"))
                .andExpect(jsonPath("$.expiresIn").value(8 * 3600))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        for (var modulo : MODULO.entrySet()) {
            acessar(bearer, modulo.getValue())
                    .andExpect(modulo.getKey() == dep ? status().isNotFound() : status().isForbidden());
        }
        acessar(bearer, "/api/app/sonda").andExpect(status().isForbidden());
        loginApp(fun.matricula(), SENHA).andExpect(status().isUnauthorized()); // funcionário não usa o app
    }

    @Test
    void soAdministrativoGerenciaMatriculas() throws Exception {
        String email = emailUnico();
        Criada sup = criar(tokenAdmin(), funcionario(email, "SUPORTE"));
        ativar(sup.matricula(), sup.codigo(), SENHA);
        String bearer = JsonPath.read(loginPainel(email, SENHA).andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(post("/api/painel/gestao/matriculas").header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(entregador()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ regras de credencial

    @Test
    void semTokenOuTokenInvalidoRecebe401() throws Exception {
        mvc.perform(get("/api/painel/gestao/matriculas")).andExpect(status().isUnauthorized());
        acessar("token.falso.qualquer", "/api/app/sonda").andExpect(status().isUnauthorized());
    }

    @Test
    void validacoesDaMatricula() throws Exception {
        String admin = tokenAdmin();
        criarResultado(admin, """
                {"nome":"Sem Departamento","email":"%s","vinculo":"INTERNO"}""".formatted(emailUnico()))
                .andExpect(status().isBadRequest());
        criarResultado(admin, """
                {"nome":"Entregador Com Dep","vinculo":"EXTERNO","departamento":"SUPORTE"}""")
                .andExpect(status().isBadRequest());
        criarResultado(admin, """
                {"nome":"Funcionario Sem Email","vinculo":"INTERNO","departamento":"FINANCEIRO"}""")
                .andExpect(status().isBadRequest());
        criarResultado(admin, """
                {"nome":"","vinculo":"EXTERNO"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());

        String email = emailUnico();
        criar(admin, funcionario(email, "SUPORTE"));
        criarResultado(admin, funcionario(email.toUpperCase(), "FINANCEIRO")).andExpect(status().isConflict());
    }

    @Test
    void ativacaoExigeCodigoCorretoESegredoNoFormatoDoTipo() throws Exception {
        String admin = tokenAdmin();
        Criada ent = criar(admin, entregador());
        ativar(ent.matricula(), "ERRADO99", PIN).andExpect(status().isUnauthorized());
        ativar(ent.matricula(), ent.codigo(), "12345").andExpect(status().isBadRequest());   // curto
        ativar(ent.matricula(), ent.codigo(), "123456").andExpect(status().isBadRequest());  // sequência
        ativar(ent.matricula(), ent.codigo(), "777777").andExpect(status().isBadRequest());  // repetido
        ativar(ent.matricula(), ent.codigo().toLowerCase(), PIN).andExpect(status().isNoContent());
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isUnauthorized());     // código é de uso único

        Criada fun = criar(admin, funcionario(emailUnico(), "FINANCEIRO"));
        ativar(fun.matricula(), fun.codigo(), PIN).andExpect(status().isBadRequest());       // PIN não serve de senha
        ativar(fun.matricula(), fun.codigo(), "somenteletras").andExpect(status().isBadRequest());
    }

    @Test
    void cincoErrosTravamOLoginTemporariamente() throws Exception {
        Criada ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);
        for (int i = 0; i < 5; i++) {
            loginApp(ent.matricula(), "000001").andExpect(status().isUnauthorized());
        }
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized()); // PIN certo, mas travado
    }

    @Test
    void bloqueioEReemissaoDeCodigo() throws Exception {
        String admin = tokenAdmin();
        Criada ent = criar(admin, entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);

        mvc.perform(post("/api/painel/gestao/matriculas/{m}/bloqueio", ent.matricula()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized());

        String novoCodigo = JsonPath.read(mvc.perform(post("/api/painel/gestao/matriculas/{m}/codigo-ativacao", ent.matricula())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.codigoAtivacao");
        ativar(ent.matricula(), novoCodigo, "905318").andExpect(status().isNoContent());
        loginApp(ent.matricula(), "905318").andExpect(status().isOk());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized()); // PIN antigo morreu
    }

    // ------------------------------------------------------------------ apoio

    private record Criada(String matricula, String codigo) {
    }

    private String tokenAdmin() throws Exception {
        return JsonPath.read(loginPainel("admin@teste.local", "SenhaAdmin123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private Criada criar(String token, String json) throws Exception {
        String body = criarResultado(token, json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Criada(JsonPath.read(body, "$.matricula"), JsonPath.read(body, "$.codigoAtivacao"));
    }

    private ResultActions criarResultado(String token, String json) throws Exception {
        return mvc.perform(post("/api/painel/gestao/matriculas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions ativar(String matricula, String codigo, String credencial) throws Exception {
        return mvc.perform(post("/api/auth/ativar").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","codigoAtivacao":"%s","novaCredencial":"%s"}""".formatted(matricula, codigo, credencial)));
    }

    private ResultActions loginApp(String matricula, String pin) throws Exception {
        return mvc.perform(post("/api/auth/app/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","pin":"%s"}""".formatted(matricula, pin)));
    }

    private ResultActions loginPainel(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/painel/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","senha":"%s"}""".formatted(email, senha)));
    }

    private ResultActions acessar(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private static String entregador() {
        return """
                {"nome":"Entregador Teste","vinculo":"EXTERNO"}""";
    }

    private static String funcionario(String email, String departamento) {
        return """
                {"nome":"Funcionario Teste","email":"%s","vinculo":"INTERNO","departamento":"%s"}"""
                .formatted(email, departamento);
    }

    private static String emailUnico() {
        return "func-" + UUID.randomUUID() + "@rotacerta.local";
    }
}
