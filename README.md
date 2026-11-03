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

## Acesso: um tipo de credencial por vínculo

Fluxo completo em [`docs/fluxo-acesso.mmd`](docs/fluxo-acesso.mmd) (Mermaid).

| Vínculo  | Perfil (departamento) | Credencial       | Login                       | Token | Módulo / rotas                |
|----------|-----------------------|------------------|-----------------------------|-------|-------------------------------|
| EXTERNO  | ENTREGADOR            | `APP_ENTREGADOR` | matrícula `ENT-…` + PIN 6 dígitos | 12h | App do Entregador — `/api/v1/app/**` |
| INTERNO  | ADMINISTRATIVO        | `PAINEL_WEB`     | e-mail + senha              | 8h    | Gestão & Cadastros — `/api/v1/painel/gestao/**` |
| INTERNO  | SUPORTE               | `PAINEL_WEB`     | e-mail + senha              | 8h    | Operação & Despacho — `/api/v1/painel/operacao/**` |
| INTERNO  | FINANCEIRO            | `PAINEL_WEB`     | e-mail + senha              | 8h    | Financeiro & Precificação — `/api/v1/painel/financeiro/**` |
| INTERNO  | DESENVOLVIMENTO       | `PAINEL_WEB`     | e-mail + senha              | 8h    | Monitoramento Técnico — `/api/v1/painel/monitoramento/**` |

Regras:

- Token do app **não entra no painel** (403) e o do painel não entra no app — o tipo de credencial vai no token.
- Cada funcionário só entra no módulo do seu departamento.
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
  O usuário troca em `PUT /api/v1/auth/app/credencial` (entregador) ou `PUT /api/v1/auth/painel/credencial` (funcionário),
  que já devolvem o token novo. Vale para PIN e senha.
- O login devolve `credencialExpiraEm`, para o app/painel avisar antes de vencer.
- A troca também pode ser voluntária, antes dos 30 dias. A nova credencial precisa ser diferente da atual.

### Endpoints (RESTful, versionados em `/api/v1`)

Documentação completa e testável no Swagger. Caminhos relativos ao endereço base do perfil.

| Método | Caminho | O que faz | Acesso |
|---|---|---|---|
| POST | `/api/v1/auth/app/tokens` | login do entregador (matrícula + PIN) → token | público |
| POST | `/api/v1/auth/painel/tokens` | login do funcionário (e-mail + senha) → token | público |
| POST | `/api/v1/auth/ativacoes` | ativa o usuário (código → PIN/senha) — 204 | público |
| PUT | `/api/v1/auth/app/credencial` | troca o PIN (obrigatório a cada 30 dias) → token novo | público* |
| PUT | `/api/v1/auth/painel/credencial` | troca a senha (obrigatório a cada 30 dias) → token novo | público* |
| GET | `/api/v1/auth/sessao` | dados do token atual (perfil, módulo, permissões) | token |
| POST | `/api/v1/painel/gestao/usuarios` | cadastra usuário — 201 + `Location` + código de ativação | ADMINISTRATIVO |
| GET | `/api/v1/painel/gestao/usuarios?page=0&size=20&sort=nome,asc` | lista paginada | ADMINISTRATIVO |
| GET | `/api/v1/painel/gestao/usuarios/{matricula}` | detalhe | ADMINISTRATIVO |
| POST | `/api/v1/painel/gestao/usuarios/{matricula}/codigos-ativacao` | novo código (esqueceu PIN, trocou de celular, desbloqueio) — 201 | ADMINISTRATIVO |
| PUT | `/api/v1/painel/gestao/usuarios/{matricula}/bloqueio` | bloqueia — 204, idempotente | ADMINISTRATIVO |

\* público porque quem está com a senha vencida não consegue token; exige a senha/PIN atual.

Exemplos de corpo:

```json
POST /api/v1/painel/gestao/usuarios
{ "nome": "João", "vinculo": "EXTERNO" }
{ "nome": "Maria", "email": "maria@empresa.com", "vinculo": "INTERNO", "departamento": "FINANCEIRO" }

POST /api/v1/auth/ativacoes
{ "matricula": "ENT-000001", "codigoAtivacao": "R26CWD5F", "novaCredencial": "482917" }
```

## Formato de erro

Todo erro (negócio, validação, 401/403 da segurança, rota inexistente ou erro inesperado) volta no mesmo formato:

```json
{
  "timestamp": "2026-11-03T18:40:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos.",
  "caminho": "/transportadora_dev/api/v1/painel/gestao/usuarios",
  "campos": [ { "campo": "nome", "mensagem": "O nome é obrigatório." } ]
}
```

`campos` só aparece em erro de validação; `codigo` só quando o cliente precisa reagir (ex.: `CREDENCIAL_EXPIRADA`). Erro 500 não expõe stack trace (fica só no log).
