package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recurso /api/v1/usuarios (permissão usuario:gerenciar, só ADMINISTRATIVO). */
class UsuarioControllerIntegrationTest extends IntegracaoTestBase {

    @Test
    void soAdministrativoGerenciaUsuarios() throws Exception {
        String email = emailUnico();
        Criado sup = criar(tokenAdmin(), funcionario(email, "SUPORTE"));
        ativar(sup.matricula(), sup.codigo(), SENHA).andExpect(status().isCreated());

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
                .andExpect(status().isUnprocessableContent());
        criarResultado(admin, """
                {"nome":"Entregador Com Dep","vinculo":"EXTERNO","departamento":"SUPORTE"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensagem").value("Entregador (vínculo EXTERNO) não tem departamento."));
        criarResultado(admin, """
                {"nome":"Funcionario Sem Email","vinculo":"INTERNO","departamento":"FINANCEIRO"}""")
                .andExpect(status().isUnprocessableContent());
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
    void bloqueioPorPatchEReemissaoDeCodigo() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());
        ativar(ent.matricula(), ent.codigo(), PIN);

        bloquear(admin, ent.matricula())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOQUEADO"));
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized());

        String novoCodigo = JsonPath.read(mvc.perform(post(USUARIOS + "/{m}/codigos-ativacao", ent.matricula())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.codigoAtivacao");
        ativar(ent.matricula(), novoCodigo, "905318").andExpect(status().isCreated());
        loginApp(ent.matricula(), "905318").andExpect(status().isOk());
        loginApp(ent.matricula(), PIN).andExpect(status().isUnauthorized()); // PIN antigo morreu
    }

    @Test
    void bloquearEhIdempotente() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());
        for (int i = 0; i < 2; i++) {
            bloquear(admin, ent.matricula()).andExpect(status().isOk());
        }
    }

    @Test
    void patchAlteraSoOsCamposEnviados() throws Exception {
        String admin = tokenAdmin();
        String email = emailUnico();
        Criado fun = criar(admin, funcionario(email, "SUPORTE"));

        patchUsuario(admin, fun.matricula(), """
                {"nome":"Nome Novo"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.email").value(email)); // não enviado = não muda
        mvc.perform(patch(USUARIOS + "/{m}", fun.matricula()).header("Authorization", "Bearer " + admin)
                        .contentType("application/merge-patch+json").content("""
                                {"email":"%s"}""".formatted(email.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)); // normalizado
    }

    @Test
    void regrasDoPatch() throws Exception {
        String admin = tokenAdmin();
        Criado fun = criar(admin, funcionario(emailUnico(), "SUPORTE"));
        String outroEmail = emailUnico();
        criar(admin, funcionario(outroEmail, "FINANCEIRO"));

        patchUsuario(admin, fun.matricula(), "{}").andExpect(status().isUnprocessableContent());
        patchUsuario(admin, fun.matricula(), """
                {"nome":"   "}""").andExpect(status().isUnprocessableContent());
        patchUsuario(admin, fun.matricula(), """
                {"status":"ATIVO"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("codigos-ativacao")));
        patchUsuario(admin, fun.matricula(), """
                {"email":"%s"}""".formatted(outroEmail)).andExpect(status().isConflict());
        patchUsuario(admin, fun.matricula(), """
                {"email":"nao-eh-email"}""").andExpect(status().isBadRequest());
        patchUsuario(admin, "ENT-999999", """
                {"nome":"X"}""").andExpect(status().isNotFound());
    }

    @Test
    void naoExistePutNemDelete() throws Exception {
        String admin = tokenAdmin();
        Criado ent = criar(admin, entregador());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(USUARIOS + "/{m}", ent.matricula()).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private org.springframework.test.web.servlet.ResultActions bloquear(String token, String matricula) throws Exception {
        return patchUsuario(token, matricula, """
                {"status":"BLOQUEADO"}""");
    }

    private org.springframework.test.web.servlet.ResultActions patchUsuario(String token, String matricula, String json)
            throws Exception {
        return enviar(patch(USUARIOS + "/{m}", matricula), token, json);
    }

    @Test
    void listagemEhPaginadaEOrdenavel() throws Exception {
        String admin = tokenAdmin();
        criar(admin, entregador());
        criar(admin, entregador());

        mvc.perform(get(USUARIOS).param("page", "0").param("size", "2").param("sort", "matricula,desc")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
        mvc.perform(get(USUARIOS).param("sort", "campoQueNaoExiste").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é possível ordenar por 'campoQueNaoExiste'."));
    }
}
