# Kheprix — Airflow Data Engineering

Orquestração do pipeline ETL do projeto Kheprix com **Apache Airflow**, implementado com **Modelagem Dimensional Kimball (Star Schema)** e nomenclatura de camadas inspirada na Arquitetura Medallion.

A camada raw segue a terminologia de **Staging Area do Kimball** — uma área de pouso dos dados brutos do OLTP, sem transformações de negócio. As camadas subsequentes adotam os nomes **Silver** (limpeza e padronização) e **Gold** (Star Schema: dimensões, fatos e Presentation Layer), que se alinham à convenção Medallion moderna e à estrutura incremental do pipeline.

## Acesso

```bash
docker compose up -d
```

- **URL:** http://localhost:8081
- **Usuário / Senha:** `admin` / `admin`

---

## Visão Geral do Pipeline

```
[OLTP kheprix_oltp_db]
        ↓  (schedule: */5 * * * *)
  extract_staging          DAG 1 — Staging: copia OLTP → staging sem transformação
        ↓  (TriggerDagRunOperator)
  transform_star_schema    DAG 2 — Silver + Gold: limpeza, modelagem dimensional e presentation
```

Cada camada tem responsabilidade única. Nenhuma camada pula etapas.

---

## DAG 1 — `extract_staging`

**Responsabilidade:** replicar o banco transacional para o schema `staging` do DW sem nenhuma transformação de negócio.

### Fluxo de tasks

```
verificar_dados_novos (ShortCircuitOperator)
        ↓ (skip se sem dados novos)
extract_<tabela> × 10  (paralelo)
        ↓
trigger_transform_star_schema (TriggerDagRunOperator)
```

### ShortCircuit — detecção de dados novos

Antes de extrair qualquer coisa, a DAG verifica se existe alguma tabela do OLTP com `MAX(updated_at)` posterior ao `start_date` do último run bem-sucedido da própria DAG.

**Por que `start_date` e não `end_date`?**
Registros que chegam no OLTP durante a execução da DAG têm `updated_at` entre `start_date` e `end_date`. Usar `end_date` como referência faria esses registros nunca serem capturados.

**Por que não usar `loaded_at` da staging?**
O `loaded_at` só existe após a extração rodar. Usá-lo antes causaria skip infinito quando a staging estivesse vazia.

Tabelas monitoradas (todas com `updated_at` rastreável):

```
estudos, campanhas, especies, unidades_amostrais,
eventos_amostragem, registro_ocorrencias, variaveis, valores_variaveis
```

> `colaboradores` é excluída do monitoramento — é uma join table sem `updated_at` próprio no OLTP.

### Estratégia de extração híbrida

| Tabela | Estratégia | Motivo |
|--------|-----------|--------|
| `usuarios`, `estudos`, `colaboradores`, `campanhas`, `especies`, `variaveis`, `unidades_amostrais` | Full Load | Tabelas de cadastro — volume pequeno, sem `updated_at` confiável em todas |
| `eventos_amostragem`, `registro_ocorrencias`, `valores_variaveis` | Incremental | Tabelas transacionais — crescem continuamente |

**Auto-recuperação:** se a staging de uma tabela incremental estiver vazia, força Full Load automaticamente para o primeiro ciclo.

### High-Water Mark com overlap

Na extração incremental, a janela de captura começa 10 minutos antes do `MAX(updated_at)` da staging:

```python
hwm_com_overlap = max_updated_at - timedelta(minutes=10)
```

O overlap protege contra *late-arriving data* (registros com clock skew que chegam com `updated_at` levemente anterior ao último HWM). O UPSERT posterior garante idempotência — sem duplicatas.

### Padrão de carga incremental

Usa TEMP TABLE na mesma conexão para evitar conflito com runs paralelos e garantir atomicidade:

```
1. CREATE TEMP TABLE tmp_<tabela> AS SELECT * FROM staging.<tabela> LIMIT 0
2. COPY dados para tmp_<tabela>
3. INSERT INTO staging.<tabela> ... ON CONFLICT (id) DO UPDATE SET ...
4. DROP TABLE tmp_<tabela>
5. COMMIT
```

---

## DAG 2 — `transform_star_schema`

**Responsabilidade:** transformar os dados da staging em Silver (limpeza) e Gold (Star Schema Kimball), e registrar o resultado no log de execução.

### Fluxo de tasks (com TaskGroups)

```
log_inicio
    ↓
[silver]
  ├── silver_especies
  ├── silver_variaveis → silver_valores_variaveis → silver_variaveis_agregadas
  ├── silver_estudos
  ├── silver_campanhas
  ├── silver_unidades_amostrais
  ├── silver_eventos_amostragem
  └── silver_registro_ocorrencias
    ↓ (cross-group dependencies)
[gold]
  ├── [dimensoes]
  │     ├── gold_dim_tempo
  │     ├── gold_dim_especie
  │     ├── gold_dim_variavel
  │     ├── gold_dim_estudo
  │     ├── gold_dim_campanha
  │     ├── gold_dim_unidade_amostral
  │     ├── gold_dim_evento_amostragem
  │     └── gold_dim_registro_ocorrencia
  ├── [fatos]
  │     ├── gold_fato_medicao_entomologica
  │     └── gold_fato_variaveis_unificadas
  └── [presentation]
        ├── gold_presentation_indicadores
        └── gold_presentation_analises
    ↓
log_conclusao
```

Os TaskGroups são visuais — as dependências cross-group (silver → gold) continuam explícitas no grafo e seguem a hierarquia de coleta.

### Padrão UPSERT

Todas as tasks de silver e gold usam `execute_upsert` (`utils/db_helpers.py`), que:

1. Lê o arquivo `.sql` correspondente (query SELECT)
2. Consulta `information_schema.columns` para obter as colunas da tabela destino dinamicamente
3. Monta o `INSERT ... ON CONFLICT (...) DO UPDATE SET` ou `DO NOTHING`
4. Executa via conexão direta (não `hook.run()`) para capturar o `rowcount`
5. Retorna o `rowcount` via XCom para o `log_conclusao`

### Camada Silver

Cada tabela Silver limpa uma única entidade da staging (1:1). Transformações permitidas:

- Filtrar `deleted_at IS NOT NULL` (soft delete)
- Renomear colunas para nomes padronizados
- Derivar atributos simples dentro da mesma entidade

**Decisões não-óbvias:**

| Tabela | Decisão |
|--------|---------|
| `silver_especies` | `TRIM(CONCAT(COALESCE(genero,''), ' ', COALESCE(especie,'')))` evita espaços espúrios quando um dos campos é NULL |
| `silver_variaveis` | `nivel_aplicacao` e `tipo_dado` são enums inteiros no OLTP — convertidos para strings aqui para não vazar o schema do OLTP para o Gold |
| `silver_valores_variaveis` | CAST protegido por regex (`^-?[0-9]*\.?[0-9]+$` e `^\d{4}-\d{2}-\d{2}$`) — valor malformado retorna NULL em vez de derrubar a task |
| `silver_variaveis_agregadas` | `DISTINCT ON (id_nivel_aplicacao, variavel_id) ORDER BY updated_at DESC` antes do `jsonb_object_agg` — evita colapso silencioso de chave duplicada |

### Camada Gold — Dimensões

As dimensões seguem a hierarquia de coleta do domínio:

```
dim_estudo
    └── dim_campanha            (valida fk_estudo)
            └── dim_unidade_amostral    (valida fk_campanha)
                    └── dim_evento_amostragem   (valida fk_unidade_amostral)
                            └── dim_registro_ocorrencia
```

Cada dimensão hierárquica faz JOIN com a dimensão pai para validar integridade referencial e herdar `variaveis_customizadas` agregadas do nível correspondente da Silver.

| Dimensão | Fonte Silver | Observação |
|----------|-------------|------------|
| `dim_tempo` | `silver_registro_ocorrencias` | Surrogate key `YYYYMMDD`; estações calculadas para hemisfério sul |
| `dim_especie` | `silver_especies` | — |
| `dim_variavel` | `silver_variaveis` | — |
| `dim_estudo` | `silver_estudos` | — |
| `dim_campanha` | `silver_campanhas` + `dim_estudo` + `silver_variaveis_agregadas` | Herda variáveis de nível campanha |
| `dim_unidade_amostral` | `silver_unidades_amostrais` + `dim_campanha` + `silver_variaveis_agregadas` | Herda variáveis de nível unidade |
| `dim_evento_amostragem` | `silver_eventos_amostragem` + `dim_unidade_amostral` + `silver_variaveis_agregadas` | Herda variáveis de nível evento |
| `dim_registro_ocorrencia` | `silver_registro_ocorrencias` | `ausencia_especie = true → quantidade_apurada = 0` |

### Camada Gold — Fatos

| Tabela Fato | Descrição |
|-------------|-----------|
| `fato_medicao_entomologica` | Grão: uma ocorrência. 6 FKs para o Star Schema. Coração do modelo. |
| `fato_variaveis_unificadas` | Resolve a hierarquia polimórfica EAV em 4 níveis (registro, evento, unidade, campanha) em uma tabela flat por registro. `DISTINCT ON (id_registro, id_variavel) ORDER BY updated_at DESC` garante unicidade com o valor mais recente. |

### Camada Gold — Presentation Layer (`sql/gold/presentation/`)

Tabelas físicas desnormalizadas, atualizadas via UPSERT a cada execução do pipeline. Seguem o mesmo padrão de todas as tabelas Gold (`execute_upsert`), mas com os JOINs pré-computados para consumo direto pela API.

| Tabela | PK | Uso |
|--------|----|-----|
| `indicadores_dashboard` | `id_registro` | Estrutura plana para dashboards. Filtrar por `fk_estudo` para isolamento multi-tenant. |
| `analises_estatisticas` | `(id_registro, id_variavel)` | Formato longo (EAV) para exportação a R/Excel. `fk_unidade_amostral` exposto para matriz amostras × espécies (nMDS, PCA, RDA, CCA). `id_variavel = 0` representa registros sem variável (LEFT JOIN null row). |

**Por que tabelas e não views?**

A alternativa seria implementar as views PostgreSQL (`CREATE VIEW`), que mantêm os JOINs como definição de query e sempre retornam dados frescos. Views são simples e corretas para volumes pequenos. A escolha por tabelas físicas foi deliberada pensando na volumetria do cenário real do TCC:

- **Views re-executam os JOINs a cada requisição da API** — com milhões de registros em `fato_medicao_entomologica`, cada chamada do dashboard re-faria 4-5 JOINs sobre toda a tabela
- **Tabelas físicas pagam o custo do JOIN uma vez por pipeline** — as queries de dashboard passam a ser simples `SELECT + WHERE` sobre uma tabela indexada por `fk_estudo`
- **Consistência com a arquitetura existente** — o pipeline já materializa todas as camadas via UPSERT; as tabelas de presentation seguem o mesmo padrão sem exceção

O custo é um passo a mais no pipeline (UPSERT na presentation layer após os fatos), aceito porque o ganho em tempo de resposta da API é proporcional ao volume de dados. Para volumes menores, a diferença seria imperceptível.

### Log de execução

`utils/etl_log.py` registra cada run na tabela `public.log_execucao_etl`:

| Função | Momento | O que registra |
|--------|---------|----------------|
| `registrar_inicio` | task `log_inicio` | Insere linha com status `em_andamento` |
| `registrar_conclusao` | task `log_conclusao` | Atualiza status para `concluido` + soma de rowcounts via XCom |
| `registrar_falha` | `on_failure_callback` da DAG | Atualiza status para `falhou` + task que causou a falha |

O `log_conclusao` recebe `XCOM_TASK_IDS` com os task_ids **prefixados pelos TaskGroups** (`silver.silver_especies`, `gold.dimensoes.gold_dim_especie`, etc.) para o XCom pull funcionar corretamente.

---

## Estrutura de Arquivos

```
airflow/
├── dags/
│   ├── extract_staging.py          # DAG Staging: extração OLTP → staging
│   ├── transform_star_schema.py    # DAG Silver + Gold: transformação dimensional
│   └── utils/
│       ├── db_helpers.py           # execute_upsert
│       ├── etl_log.py              # registrar_inicio / conclusao / falha
│       └── test_ingestion.py       # 14 testes de stress de integridade de ingestão
└── sql/
    ├── silver/
    │   ├── silver_estudos.sql
    │   ├── silver_campanhas.sql
    │   ├── silver_unidades_amostrais.sql
    │   ├── silver_eventos_amostragem.sql
    │   ├── silver_registro_ocorrencias.sql
    │   ├── silver_especies.sql
    │   ├── silver_variaveis.sql
    │   ├── silver_valores_variaveis.sql
    │   └── silver_variaveis_agregadas.sql
    └── gold/
        ├── dimensions/
        │   ├── dim_tempo.sql
        │   ├── dim_especie.sql
        │   ├── dim_variavel.sql
        │   ├── dim_estudo.sql
        │   ├── dim_campanha.sql
        │   ├── dim_unidade_amostral.sql
        │   ├── dim_evento_amostragem.sql
        │   └── dim_registro_ocorrencia.sql
        ├── facts/
        │   ├── fato_medicao_entomologica.sql
        │   └── fato_variaveis_unificadas.sql
        └── presentation/           # SQLs Gold: presentation layer — JOINs pré-computados (Kimball)
            ├── indicadores_dashboard.sql
            └── analises_estatisticas.sql
```

---

## Conexões Airflow

Configuradas via variáveis de ambiente no `docker-compose.yml`:

| Conn ID | Banco | Uso |
|---------|-------|-----|
| `db_oltp` | `kheprix_oltp_db` | Fonte — leitura na extração |
| `db_dw` | `kheprix_dw_db` | Destino — escrita em staging, silver e gold |

---

## Testes de Integridade

`utils/test_ingestion.py` contém 14 testes de stress executáveis diretamente no container do scheduler:

```bash
docker exec kheprix_airflow_scheduler python /opt/airflow/dags/utils/test_ingestion.py
```

Cobrem: propagação Staging → Silver → Gold, UPSERT idempotente, soft delete, ausência de espécie, EAV multi-nível, integridade da dim_tempo e log de execução.

---

## Queries de Consumo

Todas as queries partem das tabelas Gold da Presentation Layer e devem sempre incluir o filtro de segurança por estudo do usuário logado:

```sql
WHERE fk_estudo IN (
    SELECT estudo_id FROM staging.colaboradores WHERE usuario_id = :usuario_id
)
```

### Dashboard Mobile e Web — Métricas consolidadas

```sql
-- Data da última atualização dos dados analíticos
SELECT concluido_em AS atualizado_em
FROM public.log_execucao_etl
WHERE dag_id = 'transform_star_schema' AND status = 'concluido'
ORDER BY concluido_em DESC
LIMIT 1;

-- Data de início do estudo
SELECT MIN(data_inicio) AS data_inicio
FROM public.dim_campanha
WHERE fk_estudo = :estudo_id;

-- KPIs consolidados em uma query
SELECT
    COUNT(*)                                                          AS total_registros,
    COUNT(DISTINCT nome_cientifico)                                   AS total_especies_distintas,
    COUNT(DISTINCT CASE WHEN is_ameacada THEN nome_cientifico END)    AS especies_ameacadas,
    COUNT(DISTINCT CASE WHEN is_endemica THEN nome_cientifico END)    AS especies_nativas,
    SUM(quantidade)                                                    AS total_individuos
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id;
```

### Dashboard Web — Gráficos e Distribuições

```sql
-- Registros por data (série temporal)
SELECT data_registro, COUNT(*) AS total_registros
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
GROUP BY data_registro
ORDER BY data_registro;

-- Ocorrências e abundância por espécie
SELECT
    nome_cientifico,
    nome_popular,
    COUNT(*)        AS total_ocorrencias,
    SUM(quantidade) AS total_individuos
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
GROUP BY nome_cientifico, nome_popular
ORDER BY total_ocorrencias DESC;

-- Série temporal por espécie
SELECT
    data_registro,
    nome_cientifico,
    COUNT(*)        AS ocorrencias,
    SUM(quantidade) AS individuos
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
GROUP BY data_registro, nome_cientifico
ORDER BY data_registro, nome_cientifico;

-- Distribuição nativa vs não-nativa
SELECT
    CASE WHEN is_endemica THEN 'Nativa' ELSE 'Não nativa' END AS categoria,
    COUNT(DISTINCT nome_cientifico) AS total_especies
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
GROUP BY is_endemica;

-- Riqueza de espécies por data
SELECT
    data_registro,
    COUNT(DISTINCT nome_cientifico) AS especies_distintas
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
GROUP BY data_registro
ORDER BY data_registro;

-- Heatmap de registros por localização
SELECT
    latitude, longitude,
    COUNT(*)        AS ocorrencias,
    SUM(quantidade) AS individuos
FROM public.indicadores_dashboard
WHERE fk_estudo = :estudo_id
  AND latitude  IS NOT NULL
  AND longitude IS NOT NULL
GROUP BY latitude, longitude;
```

### Análises Estatísticas — Consumo via `analises_estatisticas`

Os dados são retornados em **formato longo (EAV)**. O backend Rails transforma o resultado no payload esperado pela API R antes de chamá-la.

```sql
-- Query base: dados para análise com variáveis selecionadas pelo usuário
SELECT
    id_registro, especie, abundancia,
    nivel_variavel, nome_variavel,
    valor_numerico, valor_texto, valor_data
FROM public.analises_estatisticas
WHERE id_estudo      = :estudo_id
  AND nome_variavel IN (:variaveis_selecionadas)
ORDER BY id_registro, nome_variavel;

-- Índices de diversidade: abundância total por espécie (vetor simples)
SELECT especie AS nome_especie, SUM(abundancia) AS abundancia
FROM public.analises_estatisticas
WHERE id_estudo = :estudo_id
GROUP BY especie
ORDER BY abundancia DESC;

-- Similaridade entre amostras: matriz longa amostras × espécies
-- fk_unidade_amostral disponível diretamente na tabela para pivot no backend
SELECT id_registro, especie, abundancia, fk_unidade_amostral AS amostra
FROM public.analises_estatisticas
WHERE id_estudo = :estudo_id;

-- Correlação / regressão: dois vetores alinhados por id_registro
SELECT
    x.id_registro,
    x.valor_numerico AS valor_x,
    y.valor_numerico AS valor_y
FROM public.analises_estatisticas x
JOIN public.analises_estatisticas y
    ON  y.id_registro   = x.id_registro
    AND y.id_estudo     = x.id_estudo
WHERE x.id_estudo      = :estudo_id
  AND x.nome_variavel  = :variavel_x
  AND y.nome_variavel  = :variavel_y
  AND x.valor_numerico IS NOT NULL
  AND y.valor_numerico IS NOT NULL;
-- IS NOT NULL intencional: correlação e regressão exigem pares completos — não imputar zeros.

-- Testes estatísticos: vetor de valores agrupados por categoria
SELECT valor_numerico AS valor
FROM public.analises_estatisticas
WHERE id_estudo     = :estudo_id
  AND nome_variavel = :variavel
  AND valor_numerico IS NOT NULL;
```

**Análises suportadas pela API R:**
- Diversidade: Shannon, Simpson, Margalef, Pielou, Chao 1/2, ACE, ICE, Jackknife
- Similaridade: Jaccard, Sørensen, Bray-Curtis, Morisita-Horn
- Multivariadas: nMDS, PCA, RDA, CCA
- Correlações: Pearson, Spearman, Kendall
- Regressão: Linear, GLM (Gaussiano, Gamma, Poisson, Binomial Negativa)
- Testes: Teste T, ANOVA, Kruskal-Wallis, Shapiro-Wilk, Kolmogorov-Smirnov

### Valores NULL — Tratamento nas Tabelas de Presentation

Os campos de taxonomia incompleta e flags booleanas já são tratados nas tabelas de presentation com `COALESCE`. Nenhuma query de consumo precisa tratar NULL nesses campos:

| Campo | Valor retornado pela tabela |
|-------|--------------------------|
| `nome_popular`, `ordem`, `familia`, `genero`, `status_conservacao` | `'NA'` |
| `is_endemica` | `false` |
| `is_ameacada` | `false` |
| `latitude` / `longitude` | `NULL` — filtrar na query do heatmap |
| `valor_numerico` (EAV) | `NULL` — tratar no backend antes da API R |

### Pendência — Campo `invasora`

O campo "Espécies Invasoras" não existe no OLTP. Para implementar, são necessárias 4 etapas:

1. **Migration OLTP:** `add_column :especies, :invasora, :boolean, default: false`
2. **Silver:** adicionar `COALESCE(invasora, false) AS invasora` em `silver_especies.sql`
3. **Gold:** propagar para `dim_especie.sql` e `gold/presentation/indicadores_dashboard.sql` como `is_invasora`
4. **Dashboard:** `COUNT(DISTINCT CASE WHEN is_invasora THEN nome_cientifico END) AS especies_invasoras`

---

## Multi-Tenancy

Todos os dados no DW são isolados por estudo via `fk_estudo`. Dashboards e APIs devem sempre filtrar:

```sql
WHERE fk_estudo IN (
    SELECT estudo_id FROM staging.colaboradores WHERE usuario_id = :id_logado
)
```
