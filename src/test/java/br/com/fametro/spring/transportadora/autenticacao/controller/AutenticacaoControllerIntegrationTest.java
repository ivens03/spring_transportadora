package br.com.fametro.spring.transportadora.autenticacao.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.ADMINISTRATIVO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.DESENVOLVIMENTO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.FINANCEIRO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.SUPORTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ativação, login por tipo de credencial, matriz de acesso e troca obrigatória de senha/PIN (30 dias). */
class AutenticacaoControllerIntegrationTest extends IntegracaoTestBase {

    /** Rota "sonda" de cada módulo do painel: se o perfil pode entrar, cai em 404 (ainda não existe); se não, 403. */
    private static final Map<Departamento, String> MODULO = Map.of(
            ADMINISTRATIVO, "/api/painel/gestao/sonda",
            SUPORTE, "/api/painel/operacao/sonda",
            FINANCEIRO, "/api/painel/financeiro/sonda",
            DESENVOLVIMENTO, "/api/painel/monitoramento/sonda");

    // ------------------------------------------------------------------ entregador

    @Test
    void entregadorAtivaComPinEntraNoAppENaoEntraNoPainel() throws Exception {
        Criado ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isNoContent());

        String bearer = JsonPath.read(loginApp(ent.matricula(), PIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoCredencial").value("APP_ENTREGADOR"))
                .andExpect(jsonPath("$.expiresIn").value(12 * 3600))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ENTREGADOR"))
                .andExpect(jsonPath("$.modulo").value("App do Entregador"))
                .andExpect(jsonPath("$.permissoes.length()").value(3));

        acessar(bearer, "/api/app/sonda")
                .andExpect(status().isNotFound()) // autorizado (rota ainda não existe)
                .andExpect(jsonPath("$.mensagem").value("Rota não encontrada."));
        for (String modulo : MODULO.values()) {
            acessar(bearer, modulo).andExpect(status().isForbidden()); // sem acesso a painel web
        }
        acessar(bearer, USUARIOS)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.caminho").value(USUARIOS))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void entregadorNaoUsaLoginDoPainelMesmoComEmail() throws Exception {
        String email = emailUnico();
        Criado ent = criar(tokenAdmin(), """
                {"nome":"Entregador Com Email","email":"%s","vinculo":"EXTERNO"}""".formatted(email));
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isNoContent());

        loginPainel(email, PIN)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas."));
    }

    // ------------------------------------------------------------------ funcionários

    @ParameterizedTest
    @EnumSource(Departamento.class)
    void funcionarioEntraSoNoModuloDoSeuDepartamento(Departamento dep) throws Exception {
        String email = emailUnico();
        Criado fun = criar(tokenAdmin(), funcionario(email, dep.name()));
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

    // ------------------------------------------------------------------ regras de credencial

    @Test
    void semTokenOuTokenInvalidoRecebe401ComCorpoPadrao() throws Exception {
        mvc.perform(get(USUARIOS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"))
                .andExpect(jsonPath("$.caminho").value(USUARIOS));
        acessar("token.falso.qualquer", "/api/app/sonda")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void ativacaoExigeCodigoCorretoESegredoNoFormatoDoTipo() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());
        ativar(ent.matricula(), "ERRADO99", PIN).andExpect(status().isUnauthorized());
        ativar(ent.matricula(), ent.codigo(), "12345").andExpect(status().isBadRequest());   // curto
        ativar(ent.matricula(), ent.codigo(), "123456").andExpect(status().isBadRequest());  // sequência
        ativar(ent.matricula(), ent.codigo(), "777777").andExpect(status().isBadRequest());  // repetido
        ativar(ent.matricula(), ent.codigo().toLowerCase(), PIN).andExpect(status().isNoContent());
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isUnauthorized());     // código é de uso único

        Criado fun = criar(admin, funcionario(emailUnico(), "FINANCEIRO"));
        ativar(fun.matricula(), fun.codigo(), PIN).andExpect(status().isBadRequest());       // PIN não serve de senha
        ativar(fun.matricula(), fun.codigo(), "somenteletras").andExpect(status().isBadRequest());
    }

    @Test
    void cincoErrosTravamOLoginTemporariamente() throws Exception {
        Criado ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);
        for (int i = 0; i < 5; i++) {
            loginApp(ent.matricula(), "000001").andExpect(status().isUnauthorized());
        }
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized()); // PIN certo, mas travado
    }

    // ------------------------------------------------------------------ Argon2id + salt (30 dias) + pepper

    @Test
    void senhaFicaEmArgon2idComChaveDeSaltDe30Dias() throws Exception {
        Criado ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN).andExpect(status().isNoContent());

        String hash = jdbc.queryForObject("select senha_hash from usuario where matricula = ?", String.class, ent.matricula());
        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$").doesNotContain(PIN);

        var chave = jdbc.queryForMap("""
                select c.salt, c.ativa, c.cadastrada_em, c.expira_em from seguranca.chave_salt c
                  join usuario u on u.id = c.usuario_id where u.matricula = ?""", ent.matricula());
        assertThat(chave.get("ativa")).isEqualTo(true);
        assertThat(hash).doesNotContain((String) chave.get("salt"));
        Instant cadastradaEm = ((Timestamp) chave.get("cadastrada_em")).toInstant();
        Instant expiraEm = ((Timestamp) chave.get("expira_em")).toInstant();
        assertThat(Duration.between(cadastradaEm, expiraEm)).isEqualTo(Duration.ofDays(30));

        String credencialExpiraEm = JsonPath.read(loginApp(ent.matricula(), PIN)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.credencialExpiraEm");
        assertThat(Instant.parse(credencialExpiraEm)).isEqualTo(expiraEm);
    }

    @Test
    void depoisDe30DiasEntregadorEhObrigadoATrocarOPin() throws Exception {
        Criado ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);
        vencerChaveSalt(ent.matricula());

        loginApp(ent.matricula(), PIN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAL_EXPIRADA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("/api/auth/app/trocar-pin")));
        loginApp(ent.matricula(), "000001").andExpect(status().isUnauthorized()); // PIN errado não revela o vencimento

        trocarPin(ent.matricula(), PIN, PIN).andExpect(status().isBadRequest());        // igual ao atual
        trocarPin(ent.matricula(), "000001", "905318").andExpect(status().isUnauthorized());
        trocarPin(ent.matricula(), PIN, "905318")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.credencialExpiraEm").exists());

        loginApp(ent.matricula(), "905318").andExpect(status().isOk());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized());

        // histórico: a chave vencida ficou desativada e existe uma nova ativa
        Integer ativas = jdbc.queryForObject("""
                select count(*) filter (where c.ativa) from seguranca.chave_salt c
                  join usuario u on u.id = c.usuario_id where u.matricula = ?""", Integer.class, ent.matricula());
        Integer total = jdbc.queryForObject("""
                select count(*) from seguranca.chave_salt c
                  join usuario u on u.id = c.usuario_id where u.matricula = ?""", Integer.class, ent.matricula());
        assertThat(ativas).isEqualTo(1);
        assertThat(total).isEqualTo(2);
    }

    @Test
    void depoisDe30DiasFuncionarioEhObrigadoATrocarASenha() throws Exception {
        String email = emailUnico();
        Criado fun = criar(tokenAdmin(), funcionario(email, "FINANCEIRO"));
        ativar(fun.matricula(), fun.codigo(), SENHA);
        vencerChaveSalt(fun.matricula());

        loginPainel(email, SENHA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAL_EXPIRADA"));
        trocarSenha(email, SENHA, "curta1").andExpect(status().isBadRequest()); // política continua valendo
        trocarSenha(email, SENHA, "NovaSenhaForte2026").andExpect(status().isOk());
        loginPainel(email, "NovaSenhaForte2026").andExpect(status().isOk());
    }

    @Test
    void trocaVoluntariaAntesDeVencer() throws Exception {
        Criado ent = criar(tokenAdmin(), entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);

        trocarPin(ent.matricula(), PIN, "905318").andExpect(status().isOk());
        loginApp(ent.matricula(), "905318").andExpect(status().isOk());
    }

    private ResultActions trocarPin(String matricula, String atual, String novo) throws Exception {
        return mvc.perform(post("/api/auth/app/trocar-pin").contentType(MediaType.APPLICATION_JSON).content("""
                {"matricula":"%s","pinAtual":"%s","novoPin":"%s"}""".formatted(matricula, atual, novo)));
    }

    private ResultActions trocarSenha(String email, String atual, String nova) throws Exception {
        return mvc.perform(post("/api/auth/painel/trocar-senha").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","senhaAtual":"%s","novaSenha":"%s"}""".formatted(email, atual, nova)));
    }
}
