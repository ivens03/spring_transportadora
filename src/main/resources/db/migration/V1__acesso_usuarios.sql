-- Acesso: matrículas de entregadores e funcionários com sua credencial.
-- O perfil define vínculo, tipo de credencial e módulo (ver Perfil.java e docs/fluxo-acesso.mmd).

create sequence seq_matricula_entregador start 1;
create sequence seq_matricula_funcionario start 1;

create table usuario (
    id                        bigint generated always as identity primary key,
    matricula                 varchar(20)  not null unique,
    nome                      varchar(150) not null,
    email                     varchar(200) unique,
    perfil                    varchar(30)  not null,
    status                    varchar(30)  not null,
    senha_hash                varchar(255),
    codigo_ativacao_hash      varchar(255),
    codigo_ativacao_expira_em timestamptz,
    falhas_login              integer      not null default 0,
    bloqueado_ate             timestamptz,
    ultimo_login_em           timestamptz,
    criado_em                 timestamptz  not null,
    atualizado_em             timestamptz  not null,

    constraint ck_usuario_perfil
        check (perfil in ('ENTREGADOR', 'ADMINISTRATIVO', 'SUPORTE', 'FINANCEIRO', 'DESENVOLVIMENTO')),
    constraint ck_usuario_status
        check (status in ('PENDENTE_ATIVACAO', 'ATIVO', 'BLOQUEADO')),
    -- funcionário entra no painel pelo e-mail, então é obrigatório
    constraint ck_usuario_email_funcionario
        check (perfil = 'ENTREGADOR' or email is not null),
    -- prefixo da matrícula acompanha o vínculo
    constraint ck_usuario_prefixo_matricula
        check ((perfil = 'ENTREGADOR' and matricula like 'ENT-%')
            or (perfil <> 'ENTREGADOR' and matricula like 'FUN-%'))
);

create index ix_usuario_perfil on usuario (perfil);
