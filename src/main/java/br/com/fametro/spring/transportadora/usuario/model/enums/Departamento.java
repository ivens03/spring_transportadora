package br.com.fametro.spring.transportadora.usuario.model.enums;

/** Departamento de um funcionário (vínculo INTERNO). Cada departamento vira um perfil de acesso. */
public enum Departamento {
    ADMINISTRATIVO,
    SUPORTE,
    FINANCEIRO,
    DESENVOLVIMENTO;

    public Perfil perfil() {
        return Perfil.valueOf(name());
    }
}
