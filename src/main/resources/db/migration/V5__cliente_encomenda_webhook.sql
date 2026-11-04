-- Portal do Cliente: o CLIENTE (empresa que despacha encomendas pela transportadora) entra com e-mail + senha,
-- acompanha as encomendas dele (status, onde está o veículo, previsão) e cadastra webhooks para ser avisado
-- de cada mudança. Os avisos ficam numa fila (webhook_entrega) gravada na MESMA transação da mudança
-- (outbox): se a rota mudou, o aviso existe; o envio HTTP é feito depois, com retentativa.

create sequence seq_matricula_cliente start 1;

-- novo perfil CLIENTE com matrícula CLI-xxxxxx
alter table usuario drop constraint ck_usuario_perfil;
alter table usuario add constraint ck_usuario_perfil
    check (perfil in ('ENTREGADOR', 'CLIENTE', 'ADMINISTRATIVO', 'SUPORTE', 'FINANCEIRO', 'DESENVOLVIMENTO'));

alter table usuario drop constraint ck_usuario_prefixo_matricula;
alter table usuario add constraint ck_usuario_prefixo_matricula
    check ((perfil = 'ENTREGADOR' and matricula like 'ENT-%')
        or (perfil = 'CLIENTE' and matricula like 'CLI-%')
        or (perfil not in ('ENTREGADOR', 'CLIENTE') and matricula like 'FUN-%'));
-- ck_usuario_email_funcionario (perfil = 'ENTREGADOR' or email is not null) já obriga e-mail do cliente (é o login).

-- dono da encomenda (opcional: carga sem cliente continua valendo, só não aparece em portal nem gera aviso)
alter table carga add column cliente_id bigint references usuario (id);
create index ix_carga_cliente on carga (cliente_id, criada_em);

-- linha do tempo da encomenda (marcos; a localização não entra aqui, só no webhook)
create table encomenda_evento (
    id          bigint generated always as identity primary key,
    carga_id    bigint       not null references carga (id),
    tipo        varchar(30)  not null,
    rota_codigo varchar(20),
    detalhe     varchar(300),
    ocorrido_em timestamptz  not null,

    constraint ck_encomenda_evento_tipo check (tipo in ('CADASTRADA', 'ROTEIRIZADA', 'SAIU_PARA_ENTREGA', 'ENTREGUE',
                                                        'NAO_ENTREGUE', 'AGUARDANDO_NOVA_ROTA', 'CANCELADA'))
);

create index ix_encomenda_evento_carga on encomenda_evento (carga_id, ocorrido_em);

-- endereço do cliente que recebe os avisos (assinados com HMAC-SHA256 usando o segredo)
create table webhook (
    id            bigint generated always as identity primary key,
    cliente_id    bigint       not null references usuario (id),
    url           varchar(500) not null,
    segredo       varchar(100) not null,
    -- eventos assinados, separados por vírgula
    eventos       varchar(400) not null,
    ativo         boolean      not null default true,
    criado_em     timestamptz  not null,
    atualizado_em timestamptz  not null,

    constraint uk_webhook_cliente_url unique (cliente_id, url)
);

-- fila de envio (outbox) + histórico das tentativas
create table webhook_entrega (
    id                   bigint generated always as identity primary key,
    webhook_id           bigint       not null references webhook (id) on delete cascade,
    carga_id             bigint       references carga (id),
    evento               varchar(30)  not null,
    payload              jsonb        not null,
    status               varchar(20)  not null,
    tentativas           integer      not null default 0,
    proxima_tentativa_em timestamptz,
    ultimo_status_http   integer,
    ultimo_erro          varchar(500),
    criada_em            timestamptz  not null,
    entregue_em          timestamptz,

    constraint ck_webhook_entrega_status check (status in ('PENDENTE', 'ENTREGUE', 'FALHOU', 'DESCARTADA')),
    constraint ck_webhook_entrega_fila   check (status <> 'PENDENTE' or proxima_tentativa_em is not null)
);

-- o despachante só olha as pendentes vencidas
create index ix_webhook_entrega_fila on webhook_entrega (proxima_tentativa_em) where status = 'PENDENTE';
create index ix_webhook_entrega_webhook on webhook_entrega (webhook_id, criada_em desc);
-- controle de frequência dos avisos de localização (um por encomenda a cada N segundos)
create index ix_webhook_entrega_localizacao on webhook_entrega (webhook_id, carga_id, criada_em desc)
    where evento = 'LOCALIZACAO';
