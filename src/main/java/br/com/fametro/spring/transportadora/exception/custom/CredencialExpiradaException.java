package br.com.fametro.spring.transportadora.exception.custom;

/**
 * Senha/PIN correto, mas a chave de salt passou dos 30 dias: o usuário precisa trocar a credencial.
 * Vira HTTP 403 com {@code codigo = CREDENCIAL_EXPIRADA} para o app/painel abrir a tela de troca.
 */
public class CredencialExpiradaException extends RuntimeException {

    public static final String CODIGO = "CREDENCIAL_EXPIRADA";

    public CredencialExpiradaException(String mensagem) {
        super(mensagem);
    }
}
