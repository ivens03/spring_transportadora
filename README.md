# RotaCerta — Logística e Rastreamento de Entregas

Projeto do MBA (Backend 03). Sistema que rastreia entregas em tempo real para uma transportadora de médio porte.

**Stack:** Java 25 · Spring Boot 4.1 · Spring Security 7 (JWT) · Argon2id (Bouncy Castle) · springdoc-openapi (Swagger) · Spring Data JPA · Flyway · PostgreSQL 17 · Testcontainers

## Arquitetura: por módulo, em camadas dentro de cada um

```
br.com.fametro.spring.transportadora
├── usuario/                     cadastro e manutenção de usuários (entregadores e funcionários)
│   ├── config/                  AdministradorInicialConfig (cria o 1º ADMINISTRATIVO na subida)
│   ├── controller/              UsuarioController
│   ├── dto/request/             UsuarioRequest
│   ├── dto/response/            UsuarioResponse, CodigoAtivacaoResponse
│   ├── model/                   Usuario
│   │   └── enums/               Perfil, Vinculo, Departamento, TipoCredencial, StatusUsuario
│   ├── repository/              UsuarioRepository
│   └── service/                 UsuarioService, CodigoAtivacao, PoliticaCredencial
├── autenticacao/                login, ativação, token e regras de acesso
│   ├── config/                  SecurityConfig (matriz de acesso), JwtConfig
│   ├── controller/              AutenticacaoController
│   ├── dto/request/             LoginAppRequest, LoginPainelRequest, AtivacaoRequest
│   ├── dto/response/            TokenResponse, SessaoResponse
│   └── service/                 AutenticacaoService, TokenService
├── seguranca/                   hash de senha/PIN: Argon2id + salt + pepper (schema seguranca do banco)
│   ├── model/                   ChaveSalt (seguranca.chave_salt)
│   ├── repository/              ChaveSaltRepository
│   └── service/                 CredencialService, Argon2Hasher
├── config/                      configurações gerais: AppConfig, OpenApiConfig (Swagger), RotaCertaProperties, VariaveisAmbienteObrigatorias
└── exception/                   tratamento de erro global (vale para todos os módulos)
    ├── custom/                  RegraNegocio 400, CredenciaisInvalidas 401, RecursoNaoEncontrado 404, Conflito 409
    ├── dto/                     ErroResponse, CampoErroResponse
    └── handler/                 GlobalExceptionHandler, SecurityExceptionHandler (401/403)
```

- Dentro do módulo: `controller` → `service` → `repository` → `model`; o service devolve DTO de response.
- Dependência entre módulos em um sentido só: `autenticacao` → `usuario` → `seguranca` (`seguranca` não conhece ninguém; guarda só o `usuarioId`).
- Próximos módulos (entregas, rastreamento, ...) seguem a mesma estrutura.

## Como rodar

Pré-requisitos: JDK 25 e Docker.

```bash
# antes: crie o .env na raiz com as variáveis da tabela abaixo (o .env NÃO vai para o git)
./mvnw spring-boot:run      # perfil dev (padrão): sobe o Postgres do compose.yaml e a API
./mvnw test                 # testes com Postgres em container (não precisa do .env)
```

### Perfis

| | **dev** (padrão) | **prod** |
|---|---|---|
| Ativar | `./mvnw spring-boot:run` | `SPRING_PROFILES_ACTIVE=prod java -jar target/spring.transportadora-*.jar` |
| Endereço base | `http://localhost:8085/transportadora_dev` | `https://<servidor>/transportadora` |
| Swagger | `/transportadora_dev/swagger-ui.html` | `/transportadora/swagger-ui.html` (desliga com `SWAGGER_ENABLED=false`) |
| Segredos | arquivo `.env` | variáveis de ambiente do servidor (não lê `.env`) |
| Banco | Postgres do `compose.yaml`, sobe sozinho | `POSTGRES_*` do servidor; pool via `DB_POOL_MAX`/`DB_POOL_MIN` |
| Log | DEBUG + SQL do Hibernate | INFO, sem SQL |
| Outros | — | desligamento gracioso, `X-Forwarded-*`, compressão, Flyway sem `clean` |

Os endpoints são os mesmos nos dois perfis; muda só o prefixo (`/transportadora_dev` ou `/transportadora`).
O banner da subida mostra o perfil (verde no dev, vermelho no prod), a URL da API e a do Swagger.
Se faltar variável obrigatória, a aplicação para logo na subida listando quais faltam.

O `.env` é lido pelo Spring (`spring.config.import=optional:file:.env[.properties]`) e pelo `docker compose`.
Em servidor, as mesmas chaves podem vir como variáveis de ambiente.

| Variável                                  | Para quê |
|-------------------------------------------|----------|
| `SERVER_PORT`                             | porta da API (dev 8085, prod 8080) |
| `POSTGRES_HOST/PORT/DB/USER/PASSWORD`     | banco (também usadas pelo `compose.yaml`) |
| `ROTACERTA_JWT_SEGREDO`                   | assinatura HMAC do JWT (≥ 32 bytes) — `openssl rand -base64 48` |
| `ROTACERTA_PEPPER`                        | pepper do Argon2id (Base64, ≥ 32 bytes) — `openssl rand -base64 32` |
| `ROTACERTA_ADMIN_EMAIL` / `ROTACERTA_ADMIN_SENHA` | primeiro ADMINISTRATIVO, criado na subida se não existir nenhum |
| `ROTACERTA_TESTE_ATIVO` | `true` liga as contas fixas de teste (só no perfil dev; prod ignora) |
| `ROTACERTA_TESTE_<PERFIL>_MATRICULA` / `_EMAIL` / `_SENHA` | conta de teste de ADMINISTRATIVO, SUPORTE, FINANCEIRO e DESENVOLVIMENTO |
| `ROTACERTA_TESTE_ENTREGADOR_MATRICULA` / `_PIN` | conta de teste do entregador (login no app) |

### Contas fixas de teste (dev)

Com `ROTACERTA_TESTE_ATIVO=true`, a subida em dev garante **uma conta por perfil** com os dados do `.env`
(senha/PIN só no `.env`). A cada subida a conta volta a ficar pronta: senha/PIN igual ao `.env`, ativa,
destravada e com chave de salt nova se a de 30 dias venceu. Em prod isso não existe (`@Profile("!prod")`
e `rotacerta.bootstrap.teste.ativo=false`).

| Perfil | Matrícula | Login |
|---|---|---|
| ADMINISTRATIVO | `FUN-TESTE-ADM` | e-mail + senha do `.env` |
| SUPORTE | `FUN-TESTE-SUP` | e-mail + senha do `.env` |
| FINANCEIRO | `FUN-TESTE-FIN` | e-mail + senha do `.env` |
| DESENVOLVIMENTO | `FUN-TESTE-DEV` | e-mail + senha do `.env` |
| ENTREGADOR | `ENT-TESTE-01` | matrícula + PIN do `.env` (app) |

## Acesso: um tipo de credencial por vínculo, permissões por perfil

Fluxo completo em [`docs/fluxo-acesso.mmd`](docs/fluxo-acesso.mmd) (Mermaid).

| Vínculo  | Perfil (departamento) | Credencial       | Login                             | Token |
|----------|-----------------------|------------------|-----------------------------------|-------|
| EXTERNO  | ENTREGADOR            | `APP_ENTREGADOR` | matrícula `ENT-…` + PIN 6 dígitos | 12h   |
| INTERNO  | ADMINISTRATIVO        | `PAINEL_WEB`     | e-mail + senha (matrícula `FUN-…`) | 8h   |
| INTERNO  | SUPORTE               | `PAINEL_WEB`     | e-mail + senha                    | 8h    |
| INTERNO  | FINANCEIRO            | `PAINEL_WEB`     | e-mail + senha                    | 8h    |
| INTERNO  | DESENVOLVIMENTO       | `PAINEL_WEB`     | e-mail + senha                    | 8h    |

O acesso é decidido pelas **permissões** que vão no token (claim `permissoes`), junto com o tipo de credencial.
Toda rota não liberada explicitamente é **negada** (deny by default: 403 com token, 401 sem token).

| Permissão | Quem tem | Libera |
|---|---|---|
| `usuario:gerenciar` | ADMINISTRATIVO | `/api/v1/usuarios/**` |
| `veiculo:gerenciar` | ADMINISTRATIVO, SUPORTE, FINANCEIRO | `/api/v1/veiculos` (cadastro e manutenção da frota) |
| `abastecimento:registrar` | ADMINISTRATIVO, SUPORTE, FINANCEIRO | `/api/v1/veiculos/{id}/abastecimentos/**` |
| `consumo:ver-metricas` | FINANCEIRO | `/api/v1/metricas-consumo` e `/api/v1/veiculos/{id}/metricas-consumo` |
| `entrega:*`, `posicao:sincronizar` | ENTREGADOR | app do entregador (próximos módulos) |
| `motorista:cadastrar`, `cliente:cadastrar` | ADMINISTRATIVO | próximos módulos |
| `entrega:cadastrar`, `mapa:acompanhar`, `ocorrencia:tratar` | SUPORTE | próximos módulos |
| `frete:*`, `desconto:*`, `faturamento:consultar` | FINANCEIRO | próximos módulos |
| `mensageria:monitorar`, `dispositivo:consultar-logs` | DESENVOLVIMENTO | próximos módulos |

Regras:

- Token do app **não entra no painel** (403) e o do painel não entra no app — o tipo de credencial vai no token.
- Token do app é mais longo (12h) por causa da conexão instável dos entregadores: evita relogin na rua.
- PIN: 6 dígitos, sem repetidos (`777777`) nem sequência (`123456`). Senha: ≥ 10 caracteres com letras e números.
- 5 tentativas erradas travam o login por 15 minutos. Mensagem de erro sempre genérica.
- Código de ativação: 8 caracteres, uso único, vale 72h; no banco fica só o hash (BCrypt).

### Senha/PIN: Argon2id + salt + pepper, com validade de 30 dias

| Peça    | Onde fica | Papel |
|---------|-----------|-------|
| **hash**   | `public.usuario.senha_hash` — `$argon2id$v=19$m=19456,t=2,p=1$<hash>` | resultado do Argon2id (parâmetros mínimos da OWASP gravados junto) |
| **salt**   | `seguranca.chave_salt` — 16 bytes aleatórios por usuário | impede tabelas prontas e hashes iguais para senhas iguais |
| **pepper** | `.env` (`ROTACERTA_PEPPER`) — nunca no banco | parâmetro *secret* do Argon2: quem roubar só o banco não consegue testar senhas |

Tabela `seguranca.chave_salt`: `salt`, `ativa` (marca a chave em uso; as anteriores ficam como histórico),
`cadastrada_em` e `expira_em` (= cadastrada + 30 dias). Só pode haver **uma chave ativa por usuário**.

- Toda ativação ou troca de senha/PIN gera uma **chave de salt nova** e desativa a anterior.
- Passou de `expira_em`: o login devolve **403** com `"codigo": "CREDENCIAL_EXPIRADA"` (só para quem acertou a senha/PIN).
  O usuário troca em `PATCH /api/v1/auth/credenciais/{matricula}` e depois faz login de novo. Vale para PIN e senha.
- O login devolve `credencialExpiraEm`, para o app/painel avisar antes de vencer.
- A troca também pode ser voluntária, antes dos 30 dias. A nova credencial precisa ser diferente da atual.

## Frota: veículos, abastecimentos e métricas de consumo

- **ADMINISTRATIVO, SUPORTE e FINANCEIRO** cadastram e mantêm os veículos e registram abastecimentos.
- **Só o FINANCEIRO** vê as métricas de consumo.
- Placa antiga (`ABC1234`) ou Mercosul (`ABC1D23`), gravada maiúscula e sem hífen; placa e RENAVAM são únicos.
- RENAVAM, ano, tipo e combustível não mudam depois do cadastro (o combustível valida o histórico).
- Abastecimento = **tanque cheio**. FLEX aceita GASOLINA ou ETANOL; veículo INATIVO não recebe abastecimento.
- O odômetro precisa ficar entre o do abastecimento anterior e o do seguinte (por data). Assim um lançamento
  atrasado é aceito, mas um km que andaria para trás é recusado. O mesmo veículo na mesma data/hora = 409 (reenvio).
- Abastecimento não tem PATCH: é registro contábil, e lançamento errado se corrige com novo lançamento.

Métricas (método do tanque cheio; período em datas inclusivas no fuso `America/Fortaleza`, padrão últimos 30 dias, máximo 366):

| Campo | Cálculo |
|---|---|
| `kmRodados` | último odômetro − primeiro odômetro do período |
| `litrosAbastecidos` / `gastoTotal` | soma de todos os abastecimentos do período |
| `precoMedioLitro` | gastoTotal / litrosAbastecidos |
| `consumoMedioKmPorLitro` | km rodados / litros a partir do 2º abastecimento (o 1º só enche o tanque de partida) |
| `custoPorKm` | valor desses mesmos abastecimentos / km rodados |
| `desvioPercentual` | (km/l − referência) / referência × 100 |
| `situacao` | `DENTRO_DO_ESPERADO`, `CONSUMO_ACIMA_DO_ESPERADO` (km/l mais de 15% abaixo da referência), `SEM_REFERENCIA`, `DADOS_INSUFICIENTES` |

A visão da frota soma os km e litros só dos veículos com km/l calculado e conta quantos estão acima do esperado.
A tolerância (`rotacerta.frota.tolerancia-consumo-percentual`) e o fuso ficam no `application.properties`.

## Endpoints (RESTful, versionados em `/api/v1`)

Documentação completa e testável no Swagger. Caminhos relativos ao endereço base do perfil.

Convenções: substantivos no plural; `POST` cria (201 + `Location`); `GET` de coleção é paginado
(`page`, `size` máx. 100, `sort`); **atualização é sempre `PATCH`** parcial (`application/json` ou
`application/merge-patch+json`), **não há `PUT`**; 400 = formato inválido, 409 = duplicado,
422 = regra de negócio violada.

| Método | Caminho | O que faz | Acesso |
|---|---|---|---|
| POST | `/api/v1/auth/app/tokens` | login do entregador (matrícula + PIN) → token | público |
| POST | `/api/v1/auth/painel/tokens` | login do funcionário (e-mail + senha) → token | público |
| POST | `/api/v1/auth/credenciais` | primeiro acesso: código de ativação → PIN/senha — 201 | público |
| PATCH | `/api/v1/auth/credenciais/{matricula}` | troca PIN/senha (obrigatória a cada 30 dias) — 200 | público* |
| GET | `/api/v1/auth/sessao` | dados do token atual (perfil, permissões) | token |
| POST | `/api/v1/usuarios` | cadastra usuário — 201 + `Location` + código de ativação | `usuario:gerenciar` |
| GET | `/api/v1/usuarios?page=0&size=20&sort=nome,asc` | lista paginada | `usuario:gerenciar` |
| GET | `/api/v1/usuarios/{matricula}` | detalhe | `usuario:gerenciar` |
| PATCH | `/api/v1/usuarios/{matricula}` | altera nome/e-mail ou bloqueia (`{"status":"BLOQUEADO"}`) | `usuario:gerenciar` |
| POST | `/api/v1/usuarios/{matricula}/codigos-ativacao` | novo código (esqueceu PIN, trocou de celular, desbloqueio) — 201 | `usuario:gerenciar` |
| POST | `/api/v1/veiculos` | cadastra veículo — 201 + `Location` | `veiculo:gerenciar` |
| GET | `/api/v1/veiculos?status=ATIVO&tipo=VAN` | lista paginada com filtros opcionais | `veiculo:gerenciar` |
| GET | `/api/v1/veiculos/{id}` | detalhe | `veiculo:gerenciar` |
| PATCH | `/api/v1/veiculos/{id}` | altera placa, marca/modelo, capacidades, referência, km (só aumenta), status | `veiculo:gerenciar` |
| POST | `/api/v1/veiculos/{id}/abastecimentos` | registra abastecimento — 201 + `Location` | `abastecimento:registrar` |
| GET | `/api/v1/veiculos/{id}/abastecimentos` | lista paginada (mais recente primeiro) | `abastecimento:registrar` |
| GET | `/api/v1/veiculos/{id}/abastecimentos/{abastecimentoId}` | detalhe | `abastecimento:registrar` |
| GET | `/api/v1/veiculos/{id}/metricas-consumo?inicio=2026-10-01&fim=2026-10-31` | métricas de um veículo | `consumo:ver-metricas` |
| GET | `/api/v1/metricas-consumo?inicio=2026-10-01&fim=2026-10-31` | totais da frota + um item por veículo | `consumo:ver-metricas` |

\* público porque quem está com a senha vencida não consegue token; exige a senha/PIN atual.

Exemplos de corpo:

```json
POST /api/v1/usuarios
{ "nome": "João", "vinculo": "EXTERNO" }
{ "nome": "Maria", "email": "maria@empresa.com", "vinculo": "INTERNO", "departamento": "FINANCEIRO" }

POST /api/v1/auth/credenciais
{ "matricula": "ENT-000001", "codigoAtivacao": "R26CWD5F", "credencial": "482917" }

PATCH /api/v1/auth/credenciais/ENT-000001
{ "credencialAtual": "482917", "novaCredencial": "905318" }

POST /api/v1/veiculos
{ "placa": "RIO2A18", "marca": "Fiat", "modelo": "Fiorino", "anoFabricacao": 2023, "tipo": "UTILITARIO",
  "combustivel": "FLEX", "capacidadeCargaKg": 650, "capacidadeVolumeM3": 3.1,
  "consumoReferenciaKmPorLitro": 11.5, "quilometragemAtual": 15230 }

POST /api/v1/veiculos/1/abastecimentos
{ "dataHora": "2026-11-03T08:15:00-03:00", "odometroKm": 15642, "litros": 38.42,
  "valorTotal": 232.44, "combustivel": "GASOLINA", "posto": "Posto Aldeota" }
```

## Formato de erro

Todo erro (negócio, validação, 401/403 da segurança, rota inexistente ou erro inesperado) volta no mesmo formato:

```json
{
  "timestamp": "2026-11-03T18:40:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos.",
  "caminho": "/transportadora_dev/api/v1/usuarios",
  "campos": [ { "campo": "nome", "mensagem": "O nome é obrigatório." } ]
}
```

`campos` só aparece em erro de validação; `codigo` só quando o cliente precisa reagir (ex.: `CREDENCIAL_EXPIRADA`). Erro 500 não expõe stack trace (fica só no log).
