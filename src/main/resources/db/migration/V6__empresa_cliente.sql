-- Empresa cliente: o CLIENTE (perfil do Portal do Cliente) passa a ter os dados da empresa — CNPJ, telefone e o
-- endereço de entrega. Na nova entrega o Suporte escolhe a empresa e o destino vem preenchido com esse endereço.
-- Só acrescenta colunas (nulas para quem não é cliente); nada é apagado.

alter table usuario add column cnpj      varchar(14);
alter table usuario add column telefone  varchar(13);
alter table usuario add column cep       varchar(8);
alter table usuario add column endereco  varchar(200);
alter table usuario add column bairro    varchar(80);
alter table usuario add column cidade    varchar(80);
alter table usuario add column uf        varchar(2);
alter table usuario add column latitude  numeric(9, 6);
alter table usuario add column longitude numeric(9, 6);

-- CNPJ único (entre os que têm) e só para cliente
create unique index ux_usuario_cnpj on usuario (cnpj) where cnpj is not null;
alter table usuario add constraint ck_usuario_cnpj_cliente check (cnpj is null or perfil = 'CLIENTE');
