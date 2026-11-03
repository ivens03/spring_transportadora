package br.com.fametro.spring.transportadora.seguranca.service;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.seguranca.model.ChaveSalt;
import br.com.fametro.spring.transportadora.seguranca.repository.ChaveSaltRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Gera e confere senhas/PINs: Argon2id + salt (chave com validade de 30 dias) + pepper.
 * Toda definição de senha/PIN (ativação ou troca) cria uma chave de salt NOVA e desativa a anterior.
 */
@Service
@RequiredArgsConstructor
public class CredencialService {

    private final ChaveSaltRepository chaveSaltRepository;
    private final Argon2Hasher argon2Hasher;
    private final RotaCertaProperties props;
    private final Clock clock;

    /** Nova chave de salt (30 dias) + hash do segredo com ela. */
    @Transactional
    public CredencialGerada gerar(Long usuarioId, String segredo) {
        Instant agora = clock.instant();
        revogar(usuarioId);
        byte[] salt = argon2Hasher.gerarSalt();
        ChaveSalt chave = chaveSaltRepository.save(
                ChaveSalt.nova(usuarioId, salt, agora, props.seguranca().validadeChaveSalt()));
        return new CredencialGerada(argon2Hasher.hash(segredo, salt), chave.getExpiraEm());
    }

    @Transactional(readOnly = true)
    public VerificacaoCredencial verificar(Long usuarioId, String segredo, String hashGravado) {
        Optional<ChaveSalt> chave = chaveSaltRepository.findByUsuarioIdAndAtivaTrue(usuarioId);
        if (chave.isEmpty() || hashGravado == null) {
            simularVerificacao(segredo);
            return VerificacaoCredencial.invalida();
        }
        if (!argon2Hasher.confere(segredo, chave.get().saltEmBytes(), hashGravado)) {
            return VerificacaoCredencial.invalida();
        }
        VerificacaoCredencial.Situacao situacao = chave.get().vencida(clock.instant())
                ? VerificacaoCredencial.Situacao.EXPIRADA
                : VerificacaoCredencial.Situacao.VALIDA;
        return new VerificacaoCredencial(situacao, chave.get().getExpiraEm());
    }

    /** Desativa a chave atual (credencial deixa de valer). Usado na reemissão de código de ativação. */
    @Transactional
    public void revogar(Long usuarioId) {
        chaveSaltRepository.findByUsuarioIdAndAtivaTrue(usuarioId).ifPresent(chave -> {
            chave.desativar(clock.instant());
            // grava já: o Hibernate faria o INSERT da chave nova antes deste UPDATE e violaria "uma ativa por usuário"
            chaveSaltRepository.flush();
        });
    }

    /** Gasta o mesmo tempo de um Argon2 real (usuário inexistente não responde mais rápido). */
    public void simularVerificacao(String segredo) {
        argon2Hasher.hash(segredo == null ? "" : segredo, argon2Hasher.gerarSalt());
    }
}
