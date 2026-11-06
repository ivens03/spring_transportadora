package br.com.fametro.spring.transportadora.usuario.controller;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST/GET /api/v1/clientes: empresa cliente com CNPJ e endereço de entrega. */
class ClienteControllerIntegrationTest extends IntegracaoTestBase {

    private static final String CLIENTES = "/api/v1/clientes";

    /** CNPJ numérico válido e diferente a cada chamada (os testes compartilham o banco). */
    private static String cnpjUnico() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 8; i++) b.append(ThreadLocalRandom.current().nextInt(10));
        b.append("0001");
        b.append(dv(b.toString(), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        b.append(dv(b.toString(), new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        return b.toString();
    }

    private static int dv(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) soma += (base.charAt(i) - '0') * pesos[i];
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    private static String empresa(String cnpj, String email) {
        return """
                {"nome":"Academia Gaviões Ltda","cnpj":"%s","email":"%s","telefone":"85999990000","cep":"60325001",
                 "endereco":"Av. Bezerra de Menezes, 1496","bairro":"São Gerardo","cidade":"Fortaleza","uf":"ce",
                 "latitude":-3.733676,"longitude":-38.558170}""".formatted(cnpj, email);
    }

    @Test
    void suporteCadastraEmpresaEElaApareceComEnderecoNaLista() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        String cnpj = cnpjUnico();
        String formatado = cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);

        String corpo = enviar(post(CLIENTES), suporte, empresa(formatado, emailUnico()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matricula", startsWith("CLI-")))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.codigoAtivacao").isString())
                .andReturn().getResponse().getContentAsString();
        String matricula = JsonPath.read(corpo, "$.matricula");

        acessar(suporte, CLIENTES + "?size=500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.matricula=='%s')].cnpj".formatted(matricula), hasItem(cnpj)))
                .andExpect(jsonPath("$.content[?(@.matricula=='%s')].uf".formatted(matricula), hasItem("CE")))
                .andExpect(jsonPath("$.content[?(@.matricula=='%s')].latitude".formatted(matricula), hasItem(-3.733676)));

        // a entrega para essa empresa: carga com o cliente vinculado
        enviar(post(CARGAS), suporte, carga("São Gerardo", "-3.733676", "-38.558170", "10").replace(
                "\"longitude\":-38.558170}", "\"longitude\":-38.558170,\"clienteMatricula\":\"" + matricula + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteMatricula").value(matricula));
    }

    @Test
    void aceitaCnpjAlfanumerico() throws Exception {
        // 12ABC34501DE35 é o exemplo da Receita; pode já existir de outra execução no mesmo banco
        enviar(post(CLIENTES), tokenAdmin(), empresa("12.ABC.345/01DE-35", emailUnico()))
                .andExpect(r -> org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isIn(201, 409));
    }

    @Test
    void cnpjComDigitoErradoOuRepetido() throws Exception {
        String suporte = tokenFuncionario("SUPORTE");
        String cnpj = cnpjUnico();
        String errado = cnpj.substring(0, 13) + ((cnpj.charAt(13) - '0' + 1) % 10);
        enviar(post(CLIENTES), suporte, empresa(errado, emailUnico()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem", startsWith("CNPJ inválido")));

        enviar(post(CLIENTES), suporte, empresa(cnpj, emailUnico())).andExpect(status().isCreated());
        enviar(post(CLIENTES), suporte, empresa(cnpj, emailUnico())).andExpect(status().isConflict());
    }

    @Test
    void enderecoObrigatorioEPermissoes() throws Exception {
        enviar(post(CLIENTES), tokenFuncionario("SUPORTE"), """
                {"nome":"Sem endereço","cnpj":"%s","email":"%s"}""".formatted(cnpjUnico(), emailUnico()))
                .andExpect(status().isBadRequest());
        enviar(post(CLIENTES), tokenFuncionario("FINANCEIRO"), empresa(cnpjUnico(), emailUnico()))
                .andExpect(status().isForbidden());
        enviar(post(CLIENTES), entregadorAtivo().token(), empresa(cnpjUnico(), emailUnico()))
                .andExpect(status().isForbidden());
    }
}
