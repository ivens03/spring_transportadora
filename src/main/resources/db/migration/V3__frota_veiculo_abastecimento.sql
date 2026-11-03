-- Frota: veículos (cadastrados por ADMINISTRATIVO, SUPORTE e FINANCEIRO) e abastecimentos,
-- que alimentam as métricas de consumo vistas pelo FINANCEIRO.

create table veiculo (
    id                          bigint generated always as identity primary key,
    placa                       varchar(7)    not null unique,
    renavam                     varchar(11)   unique,
    marca                       varchar(60)   not null,
    modelo                      varchar(80)   not null,
    ano_fabricacao              integer       not null,
    tipo                        varchar(20)   not null,
    combustivel                 varchar(20)   not null,
    capacidade_carga_kg         numeric(10,2) not null,
    capacidade_volume_m3        numeric(8,2)  not null,
    consumo_referencia_km_por_litro numeric(6,2),
    quilometragem_atual         integer       not null default 0,
    status                      varchar(20)   not null,
    cadastrado_por              varchar(20)   not null,
    criado_em                   timestamptz   not null,
    atualizado_em               timestamptz   not null,

    -- placa antiga (ABC1234) ou Mercosul (ABC1D23), sempre maiúscula e sem hífen
    constraint ck_veiculo_placa       check (placa ~ '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$'),
    constraint ck_veiculo_renavam     check (renavam is null or renavam ~ '^[0-9]{11}$'),
    constraint ck_veiculo_tipo        check (tipo in ('MOTO', 'CARRO', 'UTILITARIO', 'VAN', 'VUC', 'CAMINHAO')),
    constraint ck_veiculo_combustivel check (combustivel in ('GASOLINA', 'ETANOL', 'FLEX', 'DIESEL')),
    constraint ck_veiculo_status      check (status in ('ATIVO', 'EM_MANUTENCAO', 'INATIVO')),
    constraint ck_veiculo_capacidades check (capacidade_carga_kg > 0 and capacidade_volume_m3 > 0),
    constraint ck_veiculo_consumo_ref check (consumo_referencia_km_por_litro is null or consumo_referencia_km_por_litro > 0),
    constraint ck_veiculo_km          check (quilometragem_atual >= 0)
);

create table abastecimento (
    id             bigint generated always as identity primary key,
    veiculo_id     bigint        not null references veiculo (id),
    data_hora      timestamptz   not null,
    odometro_km    integer       not null,
    litros         numeric(8,3)  not null,
    valor_total    numeric(10,2) not null,
    combustivel    varchar(20)   not null,
    posto          varchar(120),
    registrado_por varchar(20)   not null,
    criado_em      timestamptz   not null,

    constraint ck_abastecimento_valores      check (odometro_km >= 0 and litros > 0 and valor_total > 0),
    constraint ck_abastecimento_combustivel  check (combustivel in ('GASOLINA', 'ETANOL', 'DIESEL')),
    -- o mesmo abastecimento reenviado (conexão instável) não entra duas vezes
    constraint uk_abastecimento_veiculo_data unique (veiculo_id, data_hora)
);

create index ix_abastecimento_data on abastecimento (data_hora);

comment on table  abastecimento             is 'Abastecimentos da frota; base das métricas de consumo (km/l, custo por km).';
comment on column abastecimento.odometro_km is 'Odômetro no momento do abastecimento (tanque cheio).';
