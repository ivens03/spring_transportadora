package br.com.fametro.spring.transportadora.seguranca.service;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste unitário do Argon2id (sem Spring, sem banco). */
class Argon2HasherTest {

    private static final String PEPPER = b64("pepper-unitario-com-mais-de-32-bytes!!");

    private static Argon2Hasher hasher(String pepper) {
        var seguranca = new RotaCertaProperties.Seguranca(pepper, Duration.ofDays(30),
                new RotaCertaProperties.Argon2(1024, 2, 1, 32, 16)); // memória baixa só para o teste ser rápido
        return new Argon2Hasher(new RotaCertaProperties(null, null, seguranca, null));
    }

    @Test
    void geraHashNoFormatoArgon2idSemGuardarSaltNemSenha() {
        Argon2Hasher h = hasher(PEPPER);
        byte[] salt = h.gerarSalt();
        String hash = h.hash("SenhaForte2026", salt);

        assertThat(hash).startsWith("$argon2id$v=19$m=1024,t=2,p=1$");
        assertThat(hash).doesNotContain("SenhaForte2026");
        assertThat(hash).doesNotContain(Base64.getEncoder().withoutPadding().encodeToString(salt));
        assertThat(h.confere("SenhaForte2026", salt, hash)).isTrue();
        assertThat(h.confere("SenhaErrada2026", salt, hash)).isFalse();
    }

    @Test
    void mesmoSegredoComSaltDiferenteGeraHashDiferente() {
        Argon2Hasher h = hasher(PEPPER);
        assertThat(h.hash("482917", h.gerarSalt())).isNotEqualTo(h.hash("482917", h.gerarSalt()));
    }

    @Test
    void semOPepperCertoOHashNaoConfere() {
        Argon2Hasher original = hasher(PEPPER);
        Argon2Hasher outroPepper = hasher(b64("outro-pepper-com-mais-de-32-bytes-xxxx"));
        byte[] salt = original.gerarSalt();
        String hash = original.hash("482917", salt);

        assertThat(outroPepper.confere("482917", salt, hash)).isFalse(); // vazou o banco, mas não o .env
    }

    @Test
    void conferenciaUsaOsParametrosGravadosNoHash() {
        byte[] salt = hasher(PEPPER).gerarSalt();
        String hashAntigo = hasher(PEPPER).hash("482917", salt);
        var maisForte = new RotaCertaProperties.Seguranca(PEPPER, Duration.ofDays(30),
                new RotaCertaProperties.Argon2(2048, 3, 1, 32, 16));
        Argon2Hasher novo = new Argon2Hasher(new RotaCertaProperties(null, null, maisForte, null));

        assertThat(novo.confere("482917", salt, hashAntigo)).isTrue(); // endurecer parâmetros não quebra senhas antigas
    }

    @Test
    void pepperInvalidoImpedeASubida() {
        assertThatThrownBy(() -> hasher("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> hasher("não é base64 !!")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> hasher(b64("curto"))).isInstanceOf(IllegalStateException.class);
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
