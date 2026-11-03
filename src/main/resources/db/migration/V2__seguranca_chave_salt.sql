-- Hash de senha/PIN passa a ser Argon2id + salt + pepper.
--   salt   -> fica nesta tabela (uma chave ATIVA por usuário, válida por 30 dias; histórico das anteriores)
--   pepper -> segredo global no .env (ROTACERTA_PEPPER), nunca no banco
--   hash   -> continua em public.usuario.senha_hash, no formato $argon2id$v=19$m=..,t=..,p=..$<hash>
-- Quando a chave vence (expira_em), o login exige a troca da senha/PIN, que gera uma chave nova.

create schema if not exists seguranca;

create table seguranca.chave_salt (
    id            bigint generated always as identity primary key,
    usuario_id    bigint      not null references public.usuario (id),
    salt          varchar(64) not null,
    ativa         boolean     not null default true,
    cadastrada_em timestamptz not null,
    expira_em     timestamptz not null,
    desativada_em timestamptz,

    constraint ck_chave_salt_validade   check (expira_em > cadastrada_em),
    constraint ck_chave_salt_desativada check (ativa or desativada_em is not null)
);

-- No máximo UMA chave ativa por usuário
create unique index ux_chave_salt_ativa_por_usuario on seguranca.chave_salt (usuario_id) where ativa;
create index ix_chave_salt_usuario on seguranca.chave_salt (usuario_id);

comment on table  seguranca.chave_salt               is 'Chaves de salt do hash Argon2id das senhas/PINs. Validade de 30 dias; vencida = troca obrigatória.';
comment on column seguranca.chave_salt.salt          is 'Salt aleatório (16 bytes) em Base64.';
comment on column seguranca.chave_salt.ativa         is 'Marca a chave em uso pelo usuário. As anteriores ficam como histórico.';
comment on column seguranca.chave_salt.cadastrada_em is 'Quando a chave foi gerada (ativação ou troca de senha/PIN).';
comment on column seguranca.chave_salt.expira_em     is 'Data final da chave (cadastrada_em + 30 dias). Depois disso o login exige troca.';
comment on column seguranca.chave_salt.desativada_em is 'Quando a chave foi substituída ou revogada.';
