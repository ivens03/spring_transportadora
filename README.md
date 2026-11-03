# RotaCerta — Logística e Rastreamento de Entregas

Projeto do MBA (Backend 03). Sistema que rastreia entregas em tempo real para uma transportadora de médio porte.

**Stack:** Java 25 · Spring Boot 4.1 · Spring Security 7 (JWT) · Spring Data JPA · Flyway · PostgreSQL 17 · Testcontainers

## Arquitetura em camadas

```
br.com.fametro.spring.transportadora
├── config/               SecurityConfig, JwtConfig, AppConfig, RotaCertaProperties, AdministradorInicialConfig
├── controller/           recebe HTTP, valida (@Valid) e delega ao service — sem regra de negócio
├── dto/
│   ├── request/          o que a API recebe (UsuarioRequest, LoginAppRequest, ...)
│   └── response/         o que a API devolve (UsuarioResponse, TokenResponse, ...) — a entidade nunca sai da API
├── service/              regras de negócio e transações
├── repository/           acesso ao banco (Spring Data JPA)
├── model/                entidades JPA
│   └── enums/            Perfil, Vinculo, Departamento, TipoCredencial, StatusUsuario
└── exception/
    ├── custom/           exceções da aplicação (RegraNegocio 400, CredenciaisInvalidas 401, RecursoNaoEncontrado 404, Conflito 409)
    ├── dto/              ErroResponse e CampoErroResponse (corpo padrão de erro)
    └── handler/          GlobalExceptionHandler (controllers) e SecurityExceptionHandler (401/403 do Spring Security)
```

Fluxo de uma requisição: `controller` → `service` → `repository` → `model`; o service devolve DTO de response.

## Como rodar

Pré-requisitos: JDK 25 e Docker.

```bash
./mvnw spring-boot:run      # sobe o Postgres do compose.yaml (porta 5433) e a API na porta 8085
./mvnw test                 # testes de integração com Postgres em container
```

Na primeira subida é criado o **primeiro Administrativo** (senão ninguém conseguiria criar matrículas):

| Variável de ambiente     | Padrão (só desenvolvimento) |
|--------------------------|-----------------------------|
| `ROTACERTA_ADMIN_EMAIL`  | `admin@rotacerta.local`     |
| `ROTACERTA_ADMIN_SENHA`  | `RotaCerta2026`             |
| `ROTACERTA_JWT_SEGREDO`  | segredo de dev (≥ 32 bytes) |
| `PORT`                   | `8085`                      |

## Acesso: um tipo de credencial por vínculo

Fluxo completo em [`docs/fluxo-acesso.mmd`](docs/fluxo-acesso.mmd) (Mermaid).

| Vínculo  | Perfil (departamento) | Credencial       | Login                       | Token | Módulo / rotas                |
|----------|-----------------------|------------------|-----------------------------|-------|-------------------------------|
| EXTERNO  | ENTREGADOR            | `APP_ENTREGADOR` | matrícula `ENT-…` + PIN 6 dígitos | 12h | App do Entregador — `/api/app/**` |
| INTERNO  | ADMINISTRATIVO        | `PAINEL_WEB`     | e-mail + senha              | 8h    | Gestão & Cadastros — `/api/painel/gestao/**` |
| INTERNO  | SUPORTE               | `PAINEL_WEB`     | e-mail + senha              | 8h    | Operação & Despacho — `/api/painel/operacao/**` |
| INTERNO  | FINANCEIRO            | `PAINEL_WEB`     | e-mail + senha              | 8h    | Financeiro & Precificação — `/api/painel/financeiro/**` |
| INTERNO  | DESENVOLVIMENTO       | `PAINEL_WEB`     | e-mail + senha              | 8h    | Monitoramento Técnico — `/api/painel/monitoramento/**` |

Regras:

- Token do app **não entra no painel** (403) e o do painel não entra no app — o tipo de credencial vai no token.
- Cada funcionário só entra no módulo do seu departamento.
- Token do app é mais longo (12h) por causa da conexão instável dos entregadores: evita relogin na rua.
- PIN: 6 dígitos, sem repetidos (`777777`) nem sequência (`123456`). Senha: ≥ 10 caracteres com letras e números.
- 5 tentativas erradas travam o login por 15 minutos. Mensagem de erro sempre genérica.
- Código de ativação: 8 caracteres, uso único, vale 72h; no banco fica só o hash (BCrypt).

### Ciclo de vida da matrícula

1. **Administrativo cadastra o usuário** — `POST /api/painel/gestao/usuarios`
   ```json
   { "nome": "João", "vinculo": "EXTERNO" }
   { "nome": "Maria", "email": "maria@empresa.com", "vinculo": "INTERNO", "departamento": "FINANCEIRO" }
   ```
   A resposta traz a matrícula e o **código de ativação** (mostrado uma única vez).
2. **Pessoa ativa** — `POST /api/auth/ativar` `{ "matricula", "codigoAtivacao", "novaCredencial" }` (PIN ou senha).
3. **Login**
   - Entregador: `POST /api/auth/app/login` `{ "matricula", "pin" }`
   - Funcionário: `POST /api/auth/painel/login` `{ "email", "senha" }`
4. **Quem sou eu** — `GET /api/auth/me` (perfil, módulo e permissões, para o front montar o menu).
5. **Manutenção (Administrativo)**
   - `GET /api/painel/gestao/usuarios` e `GET /api/painel/gestao/usuarios/{matricula}`
   - `POST /api/painel/gestao/usuarios/{matricula}/codigo-ativacao` — esqueceu PIN/senha, trocou de celular ou desbloqueio
   - `POST /api/painel/gestao/usuarios/{matricula}/bloqueio`

## Formato de erro

Todo erro (negócio, validação, 401/403 da segurança, rota inexistente ou erro inesperado) volta no mesmo formato:

```json
{
  "timestamp": "2026-11-03T18:40:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos.",
  "caminho": "/api/painel/gestao/usuarios",
  "campos": [ { "campo": "nome", "mensagem": "O nome é obrigatório." } ]
}
```

`campos` só aparece em erro de validação. Erro 500 não expõe stack trace (fica só no log).
