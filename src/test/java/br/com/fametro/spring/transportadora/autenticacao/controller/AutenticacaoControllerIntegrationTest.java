package br.com.fametro.spring.transportadora.autenticacao.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import br.com.fametro.spring.transportadora.usuario.model.enums.Departamento;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.ADMINISTRATIVO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.DESENVOLVIMENTO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.FINANCEIRO;
import static br.com.fametro.spring.transportadora.usuario.model.enums.Departamento.SUPORTE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ativação, login por tipo de credencial e matriz de acesso aos módulos. */
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
}
