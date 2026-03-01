# Kheprix Backend

API-only backend em Rails 8.0 com PostgreSQL.

## Requisitos

- Ruby 3.4+ (deve estar no PATH do sistema)
- Rails 8.0+
- PostgreSQL 17+

## Setup

### 1. Variáveis de ambiente

Copie o conteúdo do `.env.example` da raiz do projeto para um arquivo `.env`, e ajuste os valores de acordo com seu ambiente

Variáveis disponíveis:

| Variável | Descrição | Exemplo |
|---|---|---|
| `POSTGRES_USER` | Usuário do PostgreSQL | `root` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `senha_local` |
| `POSTGRES_DB` | Nome do banco | `kheprix_oltp_db` |
| `POSTGRES_HOST` | Host do banco | `localhost` |
| `POSTGRES_PORT` | Porta do banco | `5410` |
| `SMTP_HOST` | Host do servidor SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Porta SMTP | `587` |
| `SMTP_USER` | Usuário SMTP | `seu_email@gmail.com` |
| `SMTP_PASSWORD` | Senha SMTP (senha de app) | `sua_senha_de_app` |
| `SMTP_DOMAIN` | Domínio SMTP | `gmail.com` |
| `SMTP_AUTH` | Tipo de autenticação SMTP | `plain` |
| `SMTP_STARTTLS` | Habilitar STARTTLS | `true` |
| `EMAIL_REMETENTE` | Email de remetente padrão | `noreply@kheprix.com` |
| `FRONTEND_URL` | URL base do frontend | `http://localhost:5173` |
| `REDEFINICAO_SENHA_CAMINHO` | Caminho da página de redefinição | `/redefinir-senha` |

### 2. Banco de dados

Suba o PostgreSQL via Docker Compose (na raiz do projeto):

```bash
docker compose up -d
```

Crie e migre o banco:

```bash
rails db:create db:migrate
```

### 3. Dependências

```bash
bundle install
```

### 4. Servidor

```bash
rails server
```

O servidor estará disponível em `http://localhost:3000`.

## Comandos úteis

```bash
rails server          # Iniciar servidor
rails db:migrate      # Aplicar migrações pendentes
rails db:rollback     # Desfazer última migração
```

## Testes de API

Os endpoints podem ser testados via coleção Bruno na pasta `bruno/`.
