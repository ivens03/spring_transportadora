-- Roteirização: o SUPORTE cadastra as cargas, escolhe quais vão juntas (veículo + entregador)
-- e o sistema gera a rota com a ordem otimizada das paradas. O entregador inicia a rota no app,
-- envia as posições de GPS (em lote, por causa da conexão instável) e baixa cada parada.

create sequence seq_codigo_carga start 1;
create sequence seq_codigo_rota start 1;

create table carga (
    id                    bigint generated always as identity primary key,
    codigo                varchar(20)   not null unique,
    descricao             varchar(200)  not null,
    peso_kg               numeric(10,2) not null,
    volume_m3             numeric(8,3)  not null,
    destinatario_nome     varchar(150)  not null,
    destinatario_telefone varchar(20),
    endereco              varchar(200)  not null,
    bairro                varchar(80)   not null,
    cidade                varchar(80)   not null,
    uf                    varchar(2)    not null,
    cep                   varchar(8),
    latitude              numeric(9,6)  not null,
    longitude             numeric(9,6)  not null,
    status                varchar(20)   not null,
    tentativas_entrega    integer       not null default 0,
    cadastrada_por        varchar(20)   not null,
    criada_em             timestamptz   not null,
    atualizada_em         timestamptz   not null,

    constraint ck_carga_status  check (status in ('AGUARDANDO_ROTA', 'ROTEIRIZADA', 'EM_TRANSITO', 'ENTREGUE', 'CANCELADA')),
    constraint ck_carga_medidas check (peso_kg > 0 and volume_m3 > 0),
    constraint ck_carga_coord   check (latitude between -90 and 90 and longitude between -180 and 180),
    constraint ck_carga_uf      check (uf ~ '^[A-Z]{2}$'),
    constraint ck_carga_cep     check (cep is null or cep ~ '^[0-9]{8}$')
);

create index ix_carga_status_cidade_bairro on carga (status, cidade, bairro);

create table rota (
    id                    bigint generated always as identity primary key,
    codigo                varchar(20)   not null unique,
    veiculo_id            bigint        not null references veiculo (id),
    entregador_id         bigint        not null references usuario (id),
    status                varchar(20)   not null,
    origem_latitude       numeric(9,6)  not null,
    origem_longitude      numeric(9,6)  not null,
    retorna_origem        boolean       not null,
    distancia_estimada_km numeric(8,2)  not null,
    duracao_estimada_min  integer       not null,
    peso_total_kg         numeric(10,2) not null,
    volume_total_m3       numeric(8,3)  not null,
    criada_por            varchar(20)   not null,
    criada_em             timestamptz   not null,
    iniciada_em           timestamptz,
    concluida_em          timestamptz,
    cancelada_em          timestamptz,
    atualizada_em         timestamptz   not null,

    constraint ck_rota_status check (status in ('PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'))
);

-- um veículo e um entregador só podem ter UMA rota ativa (planejada ou em andamento) por vez
create unique index uk_rota_veiculo_ativa    on rota (veiculo_id)    where status in ('PLANEJADA', 'EM_ANDAMENTO');
create unique index uk_rota_entregador_ativa on rota (entregador_id) where status in ('PLANEJADA', 'EM_ANDAMENTO');
create index ix_rota_status on rota (status);

create table parada_rota (
    id                       bigint generated always as identity primary key,
    rota_id                  bigint       not null references rota (id),
    carga_id                 bigint       not null references carga (id),
    ordem                    integer      not null,
    distancia_do_anterior_km numeric(8,2) not null,
    status                   varchar(20)  not null,
    motivo                   varchar(300),
    finalizada_em            timestamptz,

    constraint ck_parada_status check (status in ('PENDENTE', 'ENTREGUE', 'NAO_ENTREGUE')),
    constraint ck_parada_motivo check (status <> 'NAO_ENTREGUE' or motivo is not null),
    constraint uk_parada_ordem  unique (rota_id, ordem),
    constraint uk_parada_carga  unique (rota_id, carga_id)
);

create table posicao_rota (
    id              bigint generated always as identity primary key,
    rota_id         bigint       not null references rota (id),
    latitude        numeric(9,6) not null,
    longitude       numeric(9,6) not null,
    registrada_em   timestamptz  not null,
    velocidade_km_h numeric(5,1),
    precisao_m      numeric(7,1),
    recebida_em     timestamptz  not null,

    constraint ck_posicao_coord check (latitude between -90 and 90 and longitude between -180 and 180),
    -- o app reenvia o lote quando a conexão cai: a mesma leitura não entra duas vezes
    constraint uk_posicao_rota_instante unique (rota_id, registrada_em)
);

comment on table posicao_rota is 'Rastro GPS da rota, enviado pelo app do entregador (lote offline-first).';
