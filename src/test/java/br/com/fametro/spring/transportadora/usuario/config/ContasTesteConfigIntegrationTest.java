package br.com.fametro.spring.transportadora.usuario.config;

import br.com.fametro.spring.transportadora.IntegracaoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contas fixas de teste (uma por perfil) criadas na subida e sempre prontas para uso. */
@TestPropertySource(properties = {
        "rotacerta.bootstrap.teste.ativo=true",
        "rotacerta.bootstrap.teste.usuarios.suporte.matricula=FUN-TESTE-SUP",
        "rotacerta.bootstrap.teste.usuarios.suporte.nome=Teste Suporte",
        "rotacerta.bootstrap.teste.usuarios.suporte.email=suporte.teste@teste.local",
        "rotacerta.bootstrap.teste.usuarios.suporte.credencial=SuporteTeste2026",
        "rotacerta.bootstrap.teste.usuarios.financeiro.matricula=FUN-TESTE-FIN",
        "rotacerta.bootstrap.teste.usuarios.financeiro.email=financeiro.teste@teste.local",
        "rotacerta.bootstrap.teste.usuarios.financeiro.credencial=FinanceiroTeste2026",
        "rotacerta.bootstrap.teste.usuarios.entregador.matricula=ENT-TESTE-01",
        "rotacerta.bootstrap.teste.usuarios.entregador.credencial=482917"
})
class ContasTesteConfigIntegrationTest extends IntegracaoTestBase {

    @Autowired
    private ContasTesteConfig contasTeste;

    @Test
    void contasFixasJaEntramComOPerfilCerto() throws Exception {
        loginPainel("suporte.teste@teste.local", "SuporteTeste2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("FUN-TESTE-SUP"))
                .andExpect(jsonPath("$.perfil").value("SUPORTE"));
        loginPainel("financeiro.teste@teste.local", "FinanceiroTeste2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("FINANCEIRO"));
        loginApp("ENT-TESTE-01", "482917")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ENTREGADOR"));
        // perfis sem conta configurada são só ignorados (o admin inicial continua existindo)
        tokenAdmin();
    }

    @Test
    void naSubidaSeguinteAContaVoltaAFicarPronta() throws Exception {
        vencerChaveSalt("FUN-TESTE-SUP");
        loginPainel("suporte.teste@teste.local", "SuporteTeste2026").andExpect(status().isForbidden()); // venceu
        for (int i = 0; i < 5; i++) {
            loginApp("ENT-TESTE-01", "000001"); // trava o entregador
        }
        loginApp("ENT-TESTE-01", "482917").andExpect(status().isUnauthorized());

        contasTeste.run(null); // simula nova subida

        loginPainel("suporte.teste@teste.local", "SuporteTeste2026").andExpect(status().isOk());
        loginApp("ENT-TESTE-01", "482917").andExpect(status().isOk());
    }
}
