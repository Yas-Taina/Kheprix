# Kheprix

Sistema para análise de bioindicadores, biomonitoramento e estudo de biodiversidade na entomofauna.

## Sumário

1. [Pré-requisitos](#pré-requisitos)
2. [Setup Rápido](#setup-rápido)
3. [URLs e Portas](#urls-e-portas)
4. [Estrutura do Repositório](#estrutura-do-repositório)
5. [Como Usar](#como-usar)
6. [Módulos](#módulos)
7. [Comandos Úteis](#comandos-úteis)
8. [Troubleshooting](#troubleshooting)
9. [Equipe](#equipe)

## Pré-requisitos

| Ferramenta | Versão mínima | Como instalar |
|---|---|---|
| **Docker Desktop** | 20.10+ | https://www.docker.com/products/docker-desktop |
| **Git** | qualquer | https://git-scm.com/downloads |
| **Android Studio** *(opcional, só pra mobile)* | Hedgehog (2023.1) | https://developer.android.com/studio |

A stack inteira (web, banco, análises R, chatbot, ETL) sobe via Docker — não precisa instalar Ruby, Node, Python ou R localmente.

---

## Setup Rápido

```bash
# 1. Clone o repositório
git clone https://github.com/Yas-Taina/Kheprix.git
cd Kheprix

# 2. Configure variáveis de ambiente
cp .env.example .env
# Edite o .env e preencha:
#   - SMTP_USER e SMTP_PASSWORD (para envio de e-mails de recuperação de senha)
#   - GROQ_API_KEY (chave gratuita em https://console.groq.com/keys, para o chatbot)
#   - CHATBOT_INTERNAL_KEY (gere com: python -c "import secrets; print(secrets.token_hex(32))")

# 3. Suba toda a stack
docker compose up -d --build

# 4. Aguarde tudo ficar healthy (pode demorar um pouco na primeira vez)
docker ps
# Procure status "(healthy)" nos containers críticos:
# kheprix_web_container, kheprix_oltp_database_container,
# kheprix_dw_container, kheprix_api_r_container, kheprix_frontend_container
```

Acesse **http://localhost:4200** no navegador. Faça cadastro de um usuário e comece a usar.

---

## URLs e Portas

| Serviço | URL | Credenciais |
|---|---|---|
| **Frontend (web)** | http://localhost:4200 | cadastre-se na própria interface |
| **Backend Rails API** | http://localhost:3000 | JWT após login |
| **Airflow (ETL)** | http://localhost:8081 | `admin` / `admin` |
| **Adminer (DB UI)** | http://localhost:8080 | server `db` ou `db_dw`, user/senha do `.env` |
| **API R (análises)** | http://localhost:8000 | sem auth (interno) |
| **Chatbot** | http://localhost:8001 | acessado via backend |

**Bancos de dados** (acesso direto via psql / Adminer):
- **OLTP**: `localhost:5410` — banco operacional (estudos, espécies, registros)
- **DW**: `localhost:5433` — data warehouse (dimensões, fatos, presentation layer)

---

## Estrutura do Repositório

```
Kheprix/
├── backend/         # API Rails 8 (controllers, models, services, DTOs)
├── frontend/        # SPA Angular 21
│   └── Kheprix/     # código-fonte do app
├── mobile/          # App Android nativo (Kotlin)
│   └── KheprixApp/
├── API/             # Microserviço R+Plumber para análises estatísticas
├── chatbot/         # Microserviço Python (FastAPI + Groq/Llama)
├── airflow/         # DAGs e SQL do pipeline ETL (Kimball Star Schema)
│   ├── dags/
│   └── sql/
│       ├── staging/    # Raw → Staging (extract)
│       ├── silver/     # Staging → Silver (cleanup)
│       └── gold/       # Silver → Gold (dimensões, fatos, presentation)
├── docker-compose.yml
├── seed_organico.py    # Script Python para popular o OLTP com dados de exemplo
├── .env.example
└── LEIA-ME.md          # este arquivo
```

---

## Como Usar

### 1. Criar conta e logar

1. Acesse http://localhost:4200
2. Clique em **Cadastro** → preencha nome, email e senha (mínimo 8 caracteres)
3. Faça login

### 2. Criar um estudo

1. Menu lateral → **Cadastrar Novo Estudo**
2. Dê um nome ao estudo (máx. 120 caracteres)
3. Defina **variáveis customizadas** — para cada uma, escolha:
   - **Nível**: campanha, unidade amostral, evento ou registro
   - **Tipo de dado**: texto, número, data ou verdadeiro/falso
   - Ex.: `Temperatura` (unidade, número), `Chuva` (campanha, número)
4. Salve

### 3. Hierarquia de dados

```
Estudo
└── Campanha (ex.: "Verão 2025")
    └── Unidade Amostral (ex.: "Jardim Norte" — com GPS, raio, método de coleta)
        └── Evento de Amostragem (ex.: "01/02/2025 08h-12h")
            └── Registro de Ocorrência (espécie + qtde + foto + GPS)
```

Cada nível tem seus valores das variáveis customizadas que você definiu.

### 4. Cadastrar espécies do estudo

Menu lateral → entrar em **um estudo** → **Espécies** → cadastrar com taxonomia completa (classe, ordem, família, gênero, espécie, nome popular).

### 5. Rodar análises

Menu → entrar em **um estudo** → **Análises**:
- Escolha uma das **40+ análises** disponíveis (Shannon, Simpson, Pearson, ANOVA, RDA, Jaccard, GLMs, etc.)
- Configure parâmetros (variáveis, agrupamento, fonte de dados)
- Aplique filtros opcionais (campanha, data, área geográfica)
- Clique em **Confirmar**
- Veja o gráfico interativo (Plotly) + valores numéricos
- Exporte o resultado em ZIP (JSON + XML + HTML)

### 6. Exportar dados do estudo

Menu → entrar em **um estudo** → **Exportar Dados**:
- Formato: **CSV** ou **XML**
- Agrupamento: por campanha ou unidade amostral
- Download direto

### 7. Chatbot IA (opcional)

Botão flutuante no canto inferior direito — pergunte em linguagem natural:
- "Quantas espécies foram registradas no estudo X em 2025?"
- "Qual a abundância média na campanha de verão?"

O chatbot consulta o DW e responde com base nos dados reais (read-only — não modifica nada).

### 8. Colaboração em estudos

- **Convite por email**: tela **Colaboradores** → digite email → o convidado recebe um link
- **Código de acesso**: tela **Colaboradores** → o estudo tem um código de 8 dígitos com senha → outro usuário entra com **Estudos → Ingressar via código**
- Perfis: **proprietário** (controle total) ou **colaborador** (cria/edita, não deleta)

---

## Módulos

### Backend (`backend/`)

Rails 8 API-only. Endpoints REST com autenticação JWT

### Frontend (`frontend/Kheprix/`)

Angular 21 com componentes standalone. Roteamento lazy-loaded, interceptors para JWT e tipos de dados.

- **Build local (sem Docker)**:
  ```bash
  cd frontend/Kheprix
  npm install
  npm start    # roda em http://localhost:4200
  ```

### Mobile (`mobile/KheprixApp/`)

App Android nativo em Kotlin. Funciona offline (cache local) e sincroniza com o backend quando online.

- **Build**:
  1. Abra a pasta `mobile/KheprixApp` no Android Studio
  2. Aguarde sync do Gradle
  3. Conecte um device ou emulador → **Run**
- **Apontar pro backend local**: edite `RetrofitClient.kt` para usar `http://10.0.2.2:3000` (emulador) ou IP da máquina (device físico)

### API R — Análises Estatísticas (`API/`)

Microserviço em R com Plumber expondo 52 endpoints de análises (rarefação, índices de diversidade, testes estatísticos, correlações, multivariadas, GLMs, modelos de distribuição). Usa pacotes `vegan`, `ggplot2`, `plotly`, `MASS`.

- **Acesso**: o Rails chama automaticamente quando o usuário roda uma análise. Não precisa interagir diretamente.

### Airflow ETL (`airflow/`)

Pipeline de 2 DAGs (executa a cada 5 minutos):
1. `extract_staging` — OLTP → Staging (cópia bruta)
2. `transform_star_schema` — Staging → Silver → Gold (Star Schema Kimball)

Mais detalhes em `airflow/README.md`.

### Chatbot IA (`chatbot/`)

Pipeline text-to-SQL com múltiplas camadas de guard rails. Usa Groq + Llama 3.3 70B (gratuito).

Mais detalhes em `chatbot/README.md`.

### Seed de dados (`seed_organico.py`)

Script Python para popular o OLTP com dados realistas de exemplo (estudos, campanhas, espécies, registros). Útil para demonstração.

```bash
# Com a stack rodando:
pip install psycopg2-binary requests
python seed_organico.py
```

---

## Comandos Úteis

```bash
# Subir tudo
docker compose up -d --build

# Parar tudo
docker compose down

# Parar e apagar volumes (banco zerado)
docker compose down -v

# Ver logs de um serviço
docker logs kheprix_web_container -f
docker logs kheprix_frontend_container -f

# Acessar shell do backend Rails
docker exec -it kheprix_web_container bash
./bin/rails console

# Acessar shell do banco OLTP
docker exec -it kheprix_oltp_database_container psql -U root -d kheprix_oltp_db

# Reset do DW (reaplica ETL do zero)
bash backend/.claude/testes/reset_dw.sh

# Rodar suite de testes de análises
bash backend/.claude/testes/analises.sh
```

---

## Troubleshooting

| Sintoma | Causa provável | Solução |
|---|---|---|
| `localhost:4200` não abre | Frontend ainda compilando | Aguarde 1-2 min na primeira vez, depois `docker logs kheprix_frontend_container` |
| Backend retorna 500 | Migrations não rodaram | `docker compose restart web` |
| Análise falha com "serviço de análises indisponível" | API R ainda iniciando | Aguarde até o container `kheprix_api_r_container` ficar `(healthy)` |
| Chatbot retorna erro | `GROQ_API_KEY` não configurada | Obtenha chave gratuita em https://console.groq.com/keys e atualize o `.env` |
| Email de recuperação não chega | `SMTP_*` não configurado | Use Gmail com [senha de app](https://myaccount.google.com/apppasswords); preencha no `.env` |
| Dashboard mostra zero registros | ETL ainda não rodou | Aguarde até 5 min ou force o trigger no Airflow (http://localhost:8081) |
| Conflito de porta | Porta 3000/4200/5410/etc já em uso | Pare o processo conflitante ou edite as portas no `docker-compose.yml` |

---

## Equipe

Sistema desenvolvido como Trabalho de Conclusão do curso de **Análise e Desenvolvimento de Sistemas** da **Universidade Federal do Paraná**.

- Christian dos Santos Eurinidio
- João Alberto François
- Mateus De Vita Tassote
- Yasmin Allanny Calderon Silva
- Yasmin Tainá da Silva

**Professor Orientador:** Alexander Robert Kutzke
