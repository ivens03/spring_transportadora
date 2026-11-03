package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro e manutenção de usuários (módulo Gestão & Cadastros, só ADMINISTRATIVO). */
class UsuarioControllerIntegrationTest extends IntegracaoTestBase {

    @Test
    void soAdministrativoGerenciaUsuarios() throws Exception {
        String email = emailUnico();
        Criado sup = criar(tokenAdmin(), funcionario(email, "SUPORTE"));
        ativar(sup.matricula(), sup.codigo(), SENHA);

        criarResultado(tokenPainel(email, SENHA), entregador()).andExpect(status().isForbidden());
    }

    @Test
    void administrativoConsultaUsuario() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());

        mvc.perform(get(USUARIOS + "/{m}", ent.matricula()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_ATIVACAO"))
                .andExpect(jsonPath("$.tipoCredencial").value("APP_ENTREGADOR"));
        mvc.perform(get(USUARIOS + "/ENT-999999").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuário não encontrado: ENT-999999"));
    }

    @Test
    void validacoesDoCadastro() throws Exception {
        String admin = tokenAdmin();
        criarResultado(admin, """
                {"nome":"Sem Departamento","email":"%s","vinculo":"INTERNO"}""".formatted(emailUnico()))
                .andExpect(status().isBadRequest());
        criarResultado(admin, """
                {"nome":"Entregador Com Dep","vinculo":"EXTERNO","departamento":"SUPORTE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Entregador (vínculo EXTERNO) não tem departamento."));
        criarResultado(admin, """
                {"nome":"Funcionario Sem Email","vinculo":"INTERNO","departamento":"FINANCEIRO"}""")
                .andExpect(status().isBadRequest());
        criarResultado(admin, """
                {"nome":"","email":"invalido","vinculo":"EXTERNO"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')].mensagem").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'email')].mensagem").value("E-mail inválido."));
        criarResultado(admin, """
                {"nome":"Vinculo Errado","vinculo":"TERCEIRO"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        String email = emailUnico();
        criar(admin, funcionario(email, "SUPORTE"));
        criarResultado(admin, funcionario(email.toUpperCase(), "FINANCEIRO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void bloqueioEReemissaoDeCodigo() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);

        mvc.perform(post(USUARIOS + "/{m}/bloqueio", ent.matricula()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized());

        String novoCodigo = JsonPath.read(mvc.perform(post(USUARIOS + "/{m}/codigo-ativacao", ent.matricula())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.codigoAtivacao");
        ativar(ent.matricula(), novoCodigo, "905318").andExpect(status().isNoContent());
        loginApp(ent.matricula(), "905318").andExpect(status().isOk());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized()); // PIN antigo morreu
    }
}
