# Kheprix Airflow (Data Engineering)

Este diretório contém os scripts e configurações do **Apache Airflow**, o orquestrador responsável por todo o pipeline de ETL (Extração, Transformação e Carga) do projeto Kheprix, aplicando a **Arquitetura de Medalhão (Medallion Architecture)**.

## 🚀 Como Acessar

O Airflow já é levantado automaticamente no `docker-compose.yml` da raiz do projeto. Após rodar o comando `docker compose up -d`, você pode acessar a interface web:

- **URL:** [http://localhost:8081](http://localhost:8081)
- **Usuário:** `admin`
- **Senha:** `admin`

## 📁 Estrutura de Diretórios

- `/dags/`: Contém as DAGs (Directed Acyclic Graphs) escritas em Python que automatizam as tarefas analíticas.
- `/logs/`: Arquivos de log brutais de execução das tarefas (ignorados no Git).
- `/plugins/`: Plugins extra do ecossistema Airflow.
- `/documentation/`: Documentação aprofundada das justificativas de Engenharia de Dados elaboradas para o TCC.

## ⚙️ Pipelines Ativos

### Camada Bronze (Staging) - `extract_staging.py`
Responsável por conectar-se ativamente ao banco Transacional (Kheprix OLTP) e realizar a cópia espelhada de segurança para o banco Data Warehouse (Kheprix DW) utilizando injeção otimizada (`COPY TO CSV`). 

**Funcionalidades de Destaque:**
- **Extração Híbrida Inteligente:** Carrega cadastros via *Full Load* destrutivo e popula relatórios transacionais via *Incremental Load* de 5 minutos utilizando **UPSERT Nativo** (cláusula de banco _ON CONFLICT DO UPDATE_), que é mais rápido e tolerante a furos que as exclusões forçadas em massa.
- **CDC State-Healing:** Em caso de perda local do DW, o sistema é capaz de se recuperar automaticamente regredindo a etapa para um "Rolo Compressor" preenchendo as tabelas Staging primariamente do chão até a data corrente usando marcadores cronológicos no próprio Banco, e não no clock do Airflow (`High-Water Mark`).
- **Simetria de Dados Pura:** A extração obedece as extensões nativas do ID e carimba universalmente em todas as ingestões o temporal Tracker `loaded_at`.
