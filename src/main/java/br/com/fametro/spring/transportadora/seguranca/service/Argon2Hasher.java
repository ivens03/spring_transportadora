package br.com.fametro.spring.transportadora.seguranca.service;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hash de senha/PIN com Argon2id (vencedor da Password Hashing Competition, recomendado pela OWASP).
 * <ul>
 *   <li><b>salt</b>: aleatório por usuário, guardado em {@code seguranca.chave_salt} (fora do hash);</li>
 *   <li><b>pepper</b>: segredo global do .env, passado como o parâmetro "secret" (K) do próprio Argon2 —
 *       quem roubar só o banco não consegue testar senhas.</li>
 * </ul>
 * Formato gravado: {@code =19=19456,t=2,p=1$<hash Base64>} — os parâmetros vão junto para que
 * dê para endurecer a configuração no futuro sem invalidar os hashes antigos.
 */
@Component
public class Argon2Hasher {

    private static final Pattern FORMATO =
            Pattern.compile("^\\$argon2id\\$v=19\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$([A-Za-z0-9+/]+)$");
    private static final Base64.Encoder B64 = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getDecoder();
    private static final int TAMANHO_MINIMO_PEPPER = 32;

    private final SecureRandom random = new SecureRandom();
    private final byte[] pepper;
    private final RotaCertaProperties.Argon2 parametros;
    private final int tamanhoSalt;

    public Argon2Hasher(RotaCertaProperties props) {
        this.pepper = decodificarPepper(props.seguranca().pepper());
        this.parametros = props.seguranca().argon2();
        this.tamanhoSalt = props.seguranca().argon2().tamanhoSalt();
    }

    public byte[] gerarSalt() {
        byte[] salt = new byte[tamanhoSalt];
        random.nextBytes(salt);
        return salt;
    }

    public String hash(String segredo, byte[] salt) {
        byte[] hash = calcular(segredo, salt, parametros.memoriaKib(), parametros.iteracoes(),
                parametros.paralelismo(), parametros.tamanhoHash());
        return "$argon2id$v=19$m=%d,t=%d,p=%d$%s".formatted(parametros.memoriaKib(), parametros.iteracoes(),
                parametros.paralelismo(), B64.encodeToString(hash));
    }

    /** Recalcula com os parâmetros gravados no hash e compara em tempo constante. */
    public boolean confere(String segredo, byte[] salt, String hashGravado) {
        if (segredo == null || hashGravado == null) {
            return false;
        }
        Matcher m = FORMATO.matcher(hashGravado);
        if (!m.matches()) {
            return false;
        }
        byte[] esperado = B64_DEC.decode(m.group(4));
        byte[] calculado = calcular(segredo, salt, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), esperado.length);
        return MessageDigest.isEqual(esperado, calculado);
    }

    private byte[] calcular(String segredo, byte[] salt, int memoriaKib, int iteracoes, int paralelismo, int tamanho) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoriaKib)
                .withIterations(iteracoes)
                .withParallelism(paralelismo)
                .withSalt(salt)
                .withSecret(pepper)
                .build();
        Argon2BytesGenerator gerador = new Argon2BytesGenerator();
        gerador.init(params);
        byte[] saida = new byte[tamanho];
        gerador.generateBytes(segredo.getBytes(StandardCharsets.UTF_8), saida);
        return saida;
    }

    private static byte[] decodificarPepper(String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("ROTACERTA_PEPPER não configurado (.env).");
        }
        byte[] bytes;
        try {
            bytes = B64_DEC.decode(pepper.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ROTACERTA_PEPPER deve estar em Base64 (gere com: openssl rand -base64 32).");
        }
        if (bytes.length < TAMANHO_MINIMO_PEPPER) {
            throw new IllegalStateException("ROTACERTA_PEPPER precisa ter pelo menos " + TAMANHO_MINIMO_PEPPER + " bytes.");
        }
        return bytes;
    }
}
