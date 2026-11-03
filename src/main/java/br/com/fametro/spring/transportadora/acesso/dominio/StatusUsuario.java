package br.com.fametro.spring.transportadora.acesso.dominio;

public enum StatusUsuario {
    /** Matrícula criada, aguardando a pessoa ativar com o código e definir PIN/senha. */
    PENDENTE_ATIVACAO,
    ATIVO,
    /** Bloqueada pelo administrativo. Para voltar, o administrativo reemite o código de ativação. */
    BLOQUEADO
}
