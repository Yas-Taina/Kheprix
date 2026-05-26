# Kheprix Chatbot IA

Microserviço de consulta em linguagem natural ao Data Warehouse de entomologia do Kheprix.  
Pesquisadores fazem perguntas em português e recebem respostas baseadas nos dados reais dos seus estudos.

---

## Sumário

1. [Arquitetura](#arquitetura)
2. [Fluxo de Interação](#fluxo-de-interação)
3. [Por que Groq + Llama 3.3 70B?](#por-que-groq--llama-33-70b)
4. [Segurança](#segurança)
5. [Configuração](#configuração)
6. [Executando](#executando)
7. [API](#api)
8. [Validação](#validação)
9. [Limitações](#limitações)

---

## Arquitetura

O chatbot implementa um pipeline **Text-to-SQL** com múltiplas camadas de guard rails:

```
Pergunta do pesquisador
        │
        ▼
┌─────────────────────┐
│   Guard de Entrada  │  ← detecta prompt injection, perguntas fora do domínio,
│   (3 verificações)  │     encoding malicioso; substring matching para plurais
└────────┬────────────┘
         │ pergunta válida
         ▼
┌─────────────────────┐
│  LLM (Groq/Llama)   │  ← gera SQL estruturado em JSON
│  Etapa 1: SQL Gen   │     com histórico da sessão para contexto multi-turn
└────────┬────────────┘
         │ {"sql": "SELECT ...", "explicacao": "..."}
         ▼
┌─────────────────────┐
│  Guard SQL (2 cam.) │  ← valida estrutura, multi-tenant, tabelas autorizadas,
│  sql_validator      │     funções de sistema, stacked queries
│  output_guard       │
└────────┬────────────┘
         │ SQL seguro
         ▼
┌─────────────────────┐
│  Data Warehouse     │  ← conexão read-only, filtro estudo_ids paramétrico
│  (PostgreSQL)       │     via psycopg2 — nunca interpola IDs na string SQL
└────────┬────────────┘
         │ dados brutos
         ▼
┌─────────────────────┐
│  LLM (Groq/Llama)   │  ← traduz dados para português natural
│  Etapa 2: Interpret │     com histórico da sessão para respostas coerentes
└────────┬────────────┘
         │ texto em português
         ▼
┌─────────────────────┐
│  Guards de Saída    │  ← verifica contradição dados/resposta, vazamento de
│  (3 verificações)   │     informação interna, alucinação numérica
└────────┬────────────┘
         │ resposta validada
         ▼
   Resposta ao usuário
   + dados brutos (tabela)
   + SQL executado (debug)
```

### Componentes

| Arquivo | Responsabilidade |
|---|---|
| `main.py` | Aplicação FastAPI, endpoints, lifespan |
| `config.py` | Variáveis de ambiente com validação de startup |
| `query_engine.py` | Orquestra o pipeline Text-to-SQL (geração, execução, interpretação) |
| `insights_engine.py` | Coleta de métricas predefinidas + narrativa analítica via LLM |
| `schema_context.py` | Prompts do LLM (SYSTEM_PROMPT e INTERPRETACAO_PROMPT) |
| `auth.py` | Autenticação serviço-a-serviço (X-Internal-Key) |
| `rate_limiter.py` | Limite de 10 req/min por usuário (janela deslizante) |
| `session_store.py` | Histórico de sessão em memória (multi-turn, TTL 30 min) |
| `db.py` | Connection pool read-only para o DW |
| `guards/base.py` | Tipo compartilhado `GuardResult` |
| `guards/input_guard.py` | Validação de entrada (injection, domínio, encoding) |
| `guards/sql_validator.py` | Validação de SQL gerado (DDL, multi-tenant, tabelas) |
| `guards/output_guard.py` | Validação de saída (tabelas de sistema, vazamento, alucinação) |

### Multi-turn (contexto de sessão)

O chatbot mantém um histórico das últimas **4 trocas** (perguntas + respostas) por usuário, permitindo perguntas encadeadas:

```
Pesquisador: "Quantas espécies foram registradas?"
Chatbot:     "Foram registradas 3 espécies: B0 exemplaris, B1 secundus e B2 tertius."

Pesquisador: "E dessas, quais são ameaçadas?"   ← resolve "dessas" pelo contexto
Chatbot:     "Das 3 espécies, 2 são ameaçadas: B1 secundus (VU) e B2 tertius (CR)."
```

O histórico é armazenado **em memória** (sem banco de dados) e expira após 30 minutos de inatividade. Reiniciar o container limpa todas as sessões — comportamento intencional para o contexto de TCC/desenvolvimento.

---

## Fluxo de Interação

O chatbot expõe dois endpoints com propósitos distintos (`/query` e `/insights`). O **roteamento é determinado pela interface**, não por classificação automática de intenção — uma escolha deliberada para evitar ambiguidade sem custo adicional de tokens.

### Acesso via FAB (Floating Action Button)

O chatbot é acessado por um botão flutuante fixo no canto inferior direito de todas as páginas autenticadas do frontend (exceto a própria tela do chatbot):

```
┌─────────────────────────────────────────────┐
│  [qualquer página autenticada]              │
│                                             │
│                                             │
│                                    [Botão]  │  ← FAB fixo
└─────────────────────────────────────────────┘
```

Clicar no FAB navega para `/chatbot`, que exibe a interface completa de conversa.

### Dois modos de entrada na UI

```
┌─────────────────────────────────────────────┐
│  Kheprix Chatbot                            │
│                                             │
│  [Gerar Insights]        ← botão fixo   │
│                                             │
│  ─────────────────────────────────────────  │
│  [Qual espécie foi mais coletada?   ]   │
│                                   [Enviar]  │
└─────────────────────────────────────────────┘
```

| Ação do usuário | Rails chama | Resultado |
|---|---|---|
| Clica em "Gerar Insights" | `POST /insights` | Relatório analítico estruturado |
| Digita e envia no chat | `POST /query` | Resposta a pergunta específica (Text-to-SQL) |

Essa separação garante que o relatório analítico seja sempre gerado com as queries predefinidas e auditáveis do `/insights`, enquanto o chat livre usa o pipeline Text-to-SQL do `/query`.

### Fluxo de seleção de estudos para insights

Quando o usuário clica em "Gerar Insights", o sistema apresenta uma seleção dentro do próprio chat:

```
Usuário clica [Gerar Insights]
        │
        ▼
Angular busca estudos acessíveis ao usuário via Rails
        │
        ▼
Chat exibe card com chips selecionáveis:

  ┌──────────────────────────────────────────────┐
  │  Selecione os estudos para o relatório:       │
  │                                              │
  │  [Biodiversidade Cerrado]  [Mata Atlântica]  │
  │  [Coleoptera RS 2025]                        │
  │                                              │
  │       [Gerar Insights]        [Cancelar]     │
  └──────────────────────────────────────────────┘

        │ usuário seleciona e confirma
        ▼
Rails chama POST /insights com os estudo_ids selecionados
        │
        ▼
Chat exibe relatório narrativo + métricas em tabela expansível
```

**Por que seleção por nome, não por ID?**  
IDs são informação de backend — o usuário conhece seus estudos pelo nome. O Rails resolve a tradução `nome → id` internamente antes de chamar o chatbot, que nunca expõe IDs na resposta.

**Implementação no Rails:**

```ruby
# 1. Controller busca estudos do usuário no OLTP
estudos = current_user.estudos.select(:id, :nome)
# → [{ id: 16, nome: "Biodiversidade Cerrado" }, { id: 17, nome: "Mata Atlântica" }]

# 2. Frontend renderiza chips com os nomes
# 3. Usuário seleciona → frontend envia os IDs dos estudos selecionados

# 4. Controller valida autorização (estudos pertencem ao usuário)
ids_selecionados = current_user.estudos
                               .where(id: params[:estudo_ids])
                               .pluck(:id)

# 5. Chama /insights com os IDs validados
chamar_chatbot_insights(ids_selecionados, current_user.id)
```

A validação de autorização acontece no passo 4: `current_user.estudos.where(id: ...)` garante que o usuário só acesse estudos que pode acessar, mesmo que manipule o request.

---

## Por que Groq + Llama 3.3 70B?

### Groq

| Característica | Benefício |
|---|---|
| LPU (Language Processing Unit) | Inferência ~10× mais rápida que GPU cloud típica |
| API compatível com OpenAI | Integração direta sem SDK proprietário |
| Free tier (100k tokens/dia) | Cobre o workload de desenvolvimento sem custo |
| SLA de produção | Mais estável que Cerebras/preview tiers |

**Alternativas consideradas e descartadas:**
- **Google Gemini**: Inconsistências de disponibilidade de modelos e cotas na API gratuita durante o desenvolvimento
- **Cerebras**: Limite de fila (`queue_exceeded`) no tier de preview; modelo 70B não disponível no plano gratuito
- **OpenAI GPT-4**: Pago sem free tier adequado para desenvolvimento de TCC
- **Ollama local**: Sem GPU disponível; CPU-only seria impraticável para 70B

### Llama 3.3 70B (Meta)

| Característica | Benefício |
|---|---|
| 70B parâmetros | Qualidade de geração de SQL superior a modelos menores |
| Janela de contexto 128k tokens | Comporta schema completo + histórico de sessão |
| Suporte a português | Respostas naturais sem degradação de qualidade |
| Resposta em JSON estruturado | Permite `response_format={"type": "json_object"}` |
| Open weights | Transparência e auditabilidade do modelo |

**Por que não modelos menores (7B/8B)?**  
Modelos menores (ex: `llama3.1-8b`) testados anteriormente produziam SQL incorreto com frequência (sem `STRING_AGG`, sem `DISTINCT`), respostas inconsistentes ("não foram encontrados" mesmo com dados), e dificuldade em seguir instruções complexas de múltiplas colunas. O 70B resolve esses problemas com confiabilidade adequada para um sistema de produção.

---

## Segurança

### Multi-tenant

Cada requisição inclui `estudo_ids` — lista dos estudos que o usuário autenticado pode acessar. Esses IDs são:
1. Resolvidos pelo Rails consultando a tabela `collaboradores` do OLTP
2. Passados ao chatbot como parâmetro psycopg2 — **nunca interpolados na string SQL**
3. Sempre presentes no SQL via `WHERE fk_estudo = ANY(%(estudo_ids)s)`

Um usuário não consegue acessar dados de estudos que não pertencem a ele mesmo que tente manipular a pergunta.

### Autenticação serviço-a-serviço

O chatbot não é acessível ao frontend diretamente. Apenas o backend Rails pode chamá-lo, enviando `X-Internal-Key` em cada requisição. A chave é comparada com `secrets.compare_digest` para evitar timing attacks.

```
Frontend → Rails (JWT) → Chatbot (X-Internal-Key)
                  ↑
           único ponto de entrada
```

### Guard Rails (5 camadas)

```
1. Input Guard     → prompt injection, domínio, encoding malicioso
2. SQL Validator   → DDL/DML, multi-tenant, tabelas autorizadas
3. SQL Output Guard → tabelas de sistema, funções proibidas, stacked queries
4. Resposta Final  → vazamento de SQL, variáveis internas, IDs de backend
5. Anti-alucinação → números na resposta validados contra dados reais do DW
```

#### Guard de Entrada — detalhes

O `input_guard.py` aplica três verificações em sequência:

1. **Encoding** — bloqueia caracteres de controle (`\x00`–`\x1f`, `\x7f`)
2. **Prompt injection** — detecta ~20 padrões em português e inglês (ex: "ignore as instruções anteriores", "jailbreak", "system prompt", DDL direto na pergunta)
3. **Relevância de domínio** — usa correspondência por substring para capturar plurais e variações:
   - `"estudo" in texto` captura "estudos", "do estudo", etc.
   - Perguntas de ≥ 5 palavras sem nenhum termo do domínio entomológico são bloqueadas
   - Termos explicitamente fora do domínio (culinária, entretenimento, finanças, política) bloqueiam independente do tamanho
   - **Variáveis ecológicas** (temperatura, umidade, pH, altitude, precipitação) **não** estão na lista de bloqueio — podem ser métricas legítimas de estudos de campo

#### Tratamento de erros de LLM

O pipeline distingue dois tipos de falha do Groq:

| Situação | Código HTTP | Mensagem ao usuário |
|---|---|---|
| Rate limit diário (100k tokens/dia esgotados) | 429 | "O limite de uso do serviço de IA foi atingido por hoje. Tente novamente mais tarde." |
| Erro transiente (500/502/503) | — | Retry automático até 2× com backoff de 4s |
| Erro permanente (outro) | — | "O serviço de IA está temporariamente indisponível." |
| JSON inválido gerado pelo LLM | — | "Não consegui entender a pergunta. Tente reformulá-la." |

O connection pool do DW é configurado como **read-only** via `set_session(readonly=True)`. Além disso, o chatbot usa o usuário PostgreSQL `kheprix_chatbot_ro`, que possui apenas `GRANT SELECT` nas tabelas `indicadores_dashboard` e `analises_estatisticas` — duas camadas de proteção independentes contra escrita acidental.

---

## Configuração

### Pré-requisitos

- Docker e Docker Compose
- Conta Groq gratuita: [console.groq.com](https://console.groq.com)

### Variáveis de ambiente

Copie `.env.example` para `.env` na raiz do projeto e preencha:

```env
# Groq — obtenha em console.groq.com/keys
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GROQ_MODEL=llama-3.3-70b-versatile   # padrão, pode omitir

# Chave de autenticação serviço-a-serviço (Rails → Chatbot)
# Gere com: python -c "import secrets; print(secrets.token_hex(32))"
CHATBOT_INTERNAL_KEY=<chave_hex_64_caracteres>

# URL interna do chatbot (usada pelo Rails para proxy)
CHATBOT_URL=http://chatbot:8001

# DW (valores padrão funcionam com docker-compose)
POSTGRES_DW_HOST=db_dw
POSTGRES_DW_PORT=5432
POSTGRES_DW_USER=kheprix_user
POSTGRES_DW_PASSWORD=kheprix_password
POSTGRES_DW_DB=kheprix_dw_db
```

### Gerando a chave interna

```bash
python -c "import secrets; print(secrets.token_hex(32))"
```

A mesma chave deve estar em `CHATBOT_INTERNAL_KEY` no `.env` **e** configurada no backend Rails como `CHATBOT_INTERNAL_KEY`.

---

## Executando

### Com Docker Compose (recomendado)

```bash
# Subir tudo do zero (limpa volumes e reconstrói imagens)
docker compose down -v --remove-orphans && docker compose up --build

# Subir apenas o chatbot (assume DW já rodando)
docker compose up chatbot --build -d

# Ver logs em tempo real
docker compose logs chatbot -f

# Parar
docker compose stop chatbot
```

### Verificar saúde

```bash
curl http://localhost:8001/health
# {"status": "ok", "servico": "kheprix-chatbot"}
```

---

## API

O chatbot expõe dois endpoints. **Swagger público está desabilitado** — o serviço é interno.

### `GET /health`

Verifica se o serviço está respondendo.

```bash
curl http://localhost:8001/health
```

```json
{"status": "ok", "servico": "kheprix-chatbot"}
```

### `POST /query`

Faz uma pergunta ao chatbot.

**Headers obrigatórios:**
```
Content-Type: application/json
X-Internal-Key: <CHATBOT_INTERNAL_KEY>
```

**Body:**
```json
{
  "pergunta": "Quais espécies ameaçadas foram registradas?",
  "estudo_ids": [16, 17],
  "usuario_id": 42
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `pergunta` | string (5–500 chars) | Pergunta em linguagem natural |
| `estudo_ids` | array de inteiros (mín. 1) | IDs dos estudos autorizados para o usuário |
| `usuario_id` | inteiro | ID do usuário autenticado (para rate limiting e sessão) |

**Response `200 OK`:**
```json
{
  "resposta": "Foram registradas 2 espécies ameaçadas: B1 secundus (Vulnerável) e B2 tertius (Criticamente Ameaçada).",
  "dados": [
    {"nome_cientifico": "B1 secundus", "status_conservacao": "VU", "is_ameacada": true},
    {"nome_cientifico": "B2 tertius", "status_conservacao": "CR", "is_ameacada": true}
  ],
  "sql": "SELECT DISTINCT nome_cientifico, status_conservacao, is_ameacada FROM ...",
  "total": 2,
  "erro": null
}
```

| Campo | Descrição |
|---|---|
| `resposta` | Texto em português para exibir ao usuário |
| `dados` | Registros brutos do DW (para renderizar em tabela no frontend) |
| `sql` | Query executada (para debug/transparência) |
| `total` | Número de registros retornados |
| `erro` | Mensagem de erro interno (`null` em caso de sucesso) |

**Headers de resposta:**
```
X-RateLimit-Remaining: 9   # requisições restantes no minuto atual
```

**Códigos de erro:**

| Código | Causa |
|---|---|
| `401` | `X-Internal-Key` ausente ou inválida |
| `422` | Body malformado (campo obrigatório ausente, pergunta muito curta/longa) |
| `429` | Rate limit excedido (10 req/min por `usuario_id`) — header `Retry-After: 60` |

**Exemplo com curl:**
```bash
curl -X POST http://localhost:8001/query \
  -H "Content-Type: application/json" \
  -H "X-Internal-Key: SUA_CHAVE_AQUI" \
  -d '{
    "pergunta": "Quantas espécies foram registradas?",
    "estudo_ids": [16],
    "usuario_id": 1
  }'
```

---

### `POST /insights`

Gera um relatório analítico sobre os estudos do usuário com métricas predefinidas.

Diferente de `/query`, usa SQL fixo e auditável — sem geração dinâmica de código pelo LLM.

**Headers obrigatórios:** mesmos de `/query`

**Body:**
```json
{
  "estudo_ids": [16, 17],
  "usuario_id": 42
}
```

**Response `200 OK`:**
```json
{
  "narrativa": "Visão Geral\nO estudo abrangeu 1 campanha e resultou em 30 registros...",
  "metricas": {
    "resumo":       [{"riqueza_total": 3, "abundancia_total": 395, ...}],
    "top_especies": [{"nome_cientifico": "B2 tertius", "abundancia": 180, ...}],
    "conservacao":  [{"especies_ameacadas": 2, "status_iucn": "CR, VU", ...}],
    "sazonalidade": [{"estacao": "Outono", "total_individuos": 395, ...}],
    "taxonomia":    []
  },
  "erro": null
}
```

As seções de `metricas` cobertas:
- `resumo` — riqueza, abundância, período, campanhas, nome do estudo
- `top_especies` — top 5 mais abundantes com nome popular
- `conservacao` — ameaçadas (com status IUCN), endêmicas, proporção
- `sazonalidade` — distribuição por estação do ano
- `taxonomia` — riqueza por ordem taxonômica (top 8)

A narrativa é texto puro em português (sem markdown), estruturada em 5 parágrafos correspondentes às seções acima.

---

## Validação

O script `validate_chatbot.py` executa 15 casos de teste cobrindo:

- Contagem e listagem de espécies (com nomes)
- Abundância por espécie
- Espécies ameaçadas (com status IUCN)
- Consulta de espécie específica existente
- Anti-alucinação (espécie inexistente → resposta correta "0")
- Múltiplos estudos simultaneamente
- Sazonalidade (registros por estação)
- Guard rails: domínio, prompt injection, DDL
- Qualidade: SQL não exposto na resposta
- Multi-turn: 3 perguntas encadeadas com resolução de referência pronominal

**Pré-requisito:** chatbot rodando em `localhost:8001` com dados de seed no DW.

```bash
# Rodar todos os testes (~4 minutos com delays de rate limit)
python validate_chatbot.py

# Resultado esperado
Testes basicos  : 12/12
Testes multi-turn: 3/3
Resultado final  : 15/15 casos passaram
```

> **Atenção:** o script consome ~25k tokens por execução completa.  
> Com o free tier do Groq (100k tokens/dia), execute no máximo 3–4 vezes por dia.

---

## Limitações

| Limitação | Motivo | Alternativa em produção |
|---|---|---|
| 100k tokens/dia | Free tier Groq | Upgrade para Groq Dev Tier (~$5/mês) |
| Sessões em memória | Simples, sem dependência extra | Redis para persistência entre instâncias |
| Rate limiter em memória | Idem | Redis com sliding window |
| Worker único obrigatório | session_store e rate_limiter não são thread-safe entre processos | Redis elimina o acoplamento ao processo |
| Sem streaming | FastAPI síncrono | `StreamingResponse` com SSE para respostas longas |
| Schema fixo no prompt | Prompt atualizado manualmente ao adicionar colunas | Auto-introspection via `information_schema` |
