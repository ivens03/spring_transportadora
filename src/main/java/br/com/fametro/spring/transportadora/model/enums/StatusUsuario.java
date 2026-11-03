package br.com.fametro.spring.transportadora.model.enums;

public enum StatusUsuario {
    /** Usuário criado, aguardando a pessoa ativar com o código e definir PIN/senha. */
    PENDENTE_ATIVACAO,
    ATIVO,
    /** Bloqueado pelo administrativo. Para voltar, o administrativo reemite o código de ativação. */
    BLOQUEADO
}
