class CreateDwGoldLayer < ActiveRecord::Migration[8.0]
  def change
    # =========================================================================
    # CAMADA GOLD: Modelo Dimensional (Kimball Star Schema)
    # Responsabilidade: construir dimensões com JOINs e regras de negócio,
    # e tabelas fato com métricas consolidadas.
    # =========================================================================

    # --- Dimensão Tempo ---
    create_table :dim_tempo, id: false do |t|
      t.integer :id_data, primary_key: true  # surrogate key YYYYMMDD
      t.date    :data_completa
      t.integer :dia
      t.integer :mes
      t.integer :ano
      t.integer :trimestre
      t.string  :estacao, limit: 50
    end

    # --- Dimensão Espécie ---
    create_table :dim_especie, id: false do |t|
      t.integer :id_especie, primary_key: true
      t.string  :nome_cientifico,    limit: 255
      t.string  :nome_popular,       limit: 255
      t.string  :classe,             limit: 100
      t.string  :ordem,              limit: 100
      t.string  :familia,            limit: 100
      t.string  :genero,             limit: 100
      t.boolean :endemismo,          default: false
      t.string  :status_conservacao, limit: 100
    end

    # --- Dimensão Variável (Metadata EAV) ---
    create_table :dim_variavel, id: false do |t|
      t.integer   :id, primary_key: true
      t.string    :nome,            limit: 255
      t.string    :metrica,         limit: 100
      t.string    :nivel_aplicacao, limit: 50
      t.string    :tipo_dado,       limit: 50
      t.timestamp :created_at
      t.timestamp :updated_at
    end

    # --- Nível 4: Dimensão Estudo ---
    create_table :dim_estudo, id: false do |t|
      t.integer :id_estudo, primary_key: true
      t.string  :nome_estudo, limit: 255
      t.jsonb   :variaveis_customizadas, default: {}
    end

    # --- Nível 3: Dimensão Campanha ---
    create_table :dim_campanha, id: false do |t|
      t.integer   :id_campanha, primary_key: true
      t.integer   :fk_estudo, null: false
      t.string    :nome_campanha, limit: 255
      t.date      :data_inicio
      t.timestamp :data_atualizacao
      t.text      :descricao
      t.jsonb     :variaveis_customizadas, default: {}
    end
    add_foreign_key :dim_campanha, :dim_estudo, column: :fk_estudo, primary_key: :id_estudo

    # --- Nível 2: Dimensão Unidade Amostral ---
    create_table :dim_unidade_amostral, id: false do |t|
      t.integer :id_unidade, primary_key: true
      t.integer :fk_campanha, null: false
      t.string  :nome_unidade_amostral
      t.decimal :latitude,  precision: 10, scale: 8
      t.decimal :longitude, precision: 11, scale: 8
      t.decimal :raio,      precision: 10, scale: 2
      t.text    :metodo_coleta
      t.text    :esforco_amostral
      t.jsonb   :variaveis_customizadas, default: {}
    end
    add_foreign_key :dim_unidade_amostral, :dim_campanha, column: :fk_campanha, primary_key: :id_campanha

    # --- Nível 1: Dimensão Evento Amostragem ---
    create_table :dim_evento_amostragem, id: false do |t|
      t.integer   :id_evento, primary_key: true
      t.integer   :fk_unidade_amostral, null: false
      t.timestamp :horario_inicio
      t.timestamp :data_atualizacao
      t.text      :esforco_real
      t.jsonb     :variaveis_customizadas, default: {}
    end
    add_foreign_key :dim_evento_amostragem, :dim_unidade_amostral, column: :fk_unidade_amostral, primary_key: :id_unidade

    # --- Dimensão Registro de Ocorrência ---
    create_table :dim_registro_ocorrencia, id: false do |t|
      t.integer   :id_registro, primary_key: true
      t.integer   :especie_id
      t.integer   :evento_amostragem_id
      t.date      :data
      t.decimal   :latitude,  precision: 10, scale: 8
      t.decimal   :longitude, precision: 11, scale: 8
      t.timestamp :data_atualizacao
      t.integer   :quantidade_apurada  # regra de negócio: 0 se ausencia_especie
    end

    # =========================================================================
    # CAMADA GOLD: TABELAS FATO
    # =========================================================================

    # --- Fato: Medição Entomológica (Abundância) ---
    create_table :fato_medicao_entomologica, id: false do |t|
      t.integer :id_registro,        primary_key: true
      t.integer :fk_data,            null: false
      t.integer :fk_especie,         null: false
      t.integer :fk_estudo,          null: false
      t.integer :fk_campanha,        null: false
      t.integer :fk_unidade_amostral, null: false
      t.integer :fk_evento,          null: false
      t.decimal :latitude,  precision: 10, scale: 8
      t.decimal :longitude, precision: 11, scale: 8
      t.integer :quantidade, null: false
    end

    # --- Fato: Variáveis Unificadas (EAV Gold) ---
    create_table :fato_variaveis_unificadas, id: false do |t|
      t.integer   :id_registro,       null: false
      t.integer   :id_variavel,       null: false
      t.string    :nivel_hierarquico, null: false
      t.string    :nome_variavel,     null: false
      t.decimal   :valor_numerico, precision: 18, scale: 4
      t.text      :valor_texto
      t.date      :valor_data
      t.timestamp :updated_at
    end

    # --- FKs: Medição Entomológica → Dimensões ---
    add_foreign_key :fato_medicao_entomologica, :dim_tempo,           column: :fk_data,             primary_key: :id_data
    add_foreign_key :fato_medicao_entomologica, :dim_especie,         column: :fk_especie,          primary_key: :id_especie
    add_foreign_key :fato_medicao_entomologica, :dim_estudo,          column: :fk_estudo,           primary_key: :id_estudo
    add_foreign_key :fato_medicao_entomologica, :dim_campanha,        column: :fk_campanha,         primary_key: :id_campanha
    add_foreign_key :fato_medicao_entomologica, :dim_unidade_amostral, column: :fk_unidade_amostral, primary_key: :id_unidade
    add_foreign_key :fato_medicao_entomologica, :dim_evento_amostragem, column: :fk_evento,         primary_key: :id_evento

    # --- FKs: Variáveis Unificadas ---
    add_foreign_key :fato_variaveis_unificadas, :dim_registro_ocorrencia, column: :id_registro, primary_key: :id_registro
    add_foreign_key :fato_variaveis_unificadas, :dim_variavel,              column: :id_variavel, primary_key: :id

    # --- Índices ---
    add_index :fato_variaveis_unificadas,    [:id_registro, :id_variavel], unique: true, name: 'idx_fato_vars_unique'
    add_index :fato_medicao_entomologica,    [:fk_data, :fk_especie, :fk_estudo], name: 'idx_fato_entomo_busca'
    add_index :fato_variaveis_unificadas,    :id_variavel

    # =========================================================================
    # LOG DE EXECUÇÃO DO ETL
    # =========================================================================
    create_table :log_execucao_etl do |t|
      t.string    :dag_id,                null: false
      t.string    :run_id
      t.string    :status,                null: false
      t.timestamp :iniciado_em,           null: false
      t.timestamp :concluido_em
      t.integer   :registros_processados
      t.text      :detalhes
    end
    # Constraint necessária para ON CONFLICT (dag_id, run_id) funcionar corretamente no log ETL
    add_index :log_execucao_etl, [:dag_id, :run_id], unique: true, name: 'idx_log_etl_dag_run'
  end
end
