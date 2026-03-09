class CreateKheprixDwSchema < ActiveRecord::Migration[8.0]
  def change
    # --- Dimensão Tempo ---
    # Otimizada para análise de sazonalidade
    # A opção `id: false` desabilita a chave primária auto-incremento padrão do Rails.
    create_table :dim_tempo, id: false, comment: 'Dimensão Temporal, otimizada para análise de sazonalidade.' do |t|
      t.integer :id_data, primary_key: true # YYYYMMDD
      t.date :data_completa
      t.integer :dia
      t.integer :mes
      t.integer :ano
      t.integer :trimestre
      t.string :estacao, limit: 50
    end

    # --- Dimensão Espécie ---
    # Contexto Biológico
    create_table :dim_especie, id: false, comment: 'Contexto Biológico da Espécie.' do |t|
      t.integer :id_especie, primary_key: true # Chave do catálogo OLTP
      t.string :nome_cientifico, limit: 255
      t.string :nome_popular, limit: 255
      t.string :classe, limit: 100
      t.string :status_conservacao, limit: 100
    end

    # --- HIERARQUIA SNOWFLAKE (Nível 4 -> Nível 1) ---

    # Nível 4: Estudo
    create_table :dim_estudo, id: false, comment: 'Inclui variáveis de estudo customizadas (e.g., Altitude, Tipo de Solo).' do |t|
      t.integer :id_estudo, primary_key: true
      t.string :nome_estudo, limit: 255
      t.decimal :variavel_a, precision: 10, scale: 2
      t.string :variavel_b, limit: 100
    end

    # Nível 3: Campanha
    create_table :dim_campanha, id: false, comment: 'Contexto geral da campanha. Desnormalizada na hierarquia.' do |t|
      t.integer :id_campanha, primary_key: true
      t.integer :fk_estudo, null: false
      t.date :data_inicio
      t.decimal :variavel_a, precision: 10, scale: 2
      t.string :variavel_b, limit: 100
    end
    add_foreign_key :dim_campanha, :dim_estudo, column: :fk_estudo, primary_key: :id_estudo

    # Nível 2: Unidade Amostral
    create_table :dim_unidade_amostral, id: false, comment: 'Contexto espacial e metodológico. Inclui variáveis de unidade.' do |t|
      t.integer :id_unidade, primary_key: true
      t.integer :fk_campanha, null: false
      t.decimal :latitude, precision: 10, scale: 8
      t.decimal :longitude, precision: 11, scale: 8
      t.text :metodo_coleta
      t.decimal :variavel_a, precision: 10, scale: 2
      t.string :variavel_b, limit: 100
    end
    add_foreign_key :dim_unidade_amostral, :dim_campanha, column: :fk_campanha, primary_key: :id_campanha

    # Nível 1: Evento Amostragem
    create_table :dim_evento_amostragem, id: false, comment: 'Nível mais baixo da hierarquia de coleta. Inclui variáveis instantâneas.' do |t|
      t.integer :id_evento, primary_key: true
      t.integer :fk_unidade_amostral, null: false
      t.timestamp :horario_inicio
      t.decimal :variavel_a, precision: 10, scale: 2
      t.string :variavel_b, limit: 100
    end
    add_foreign_key :dim_evento_amostragem, :dim_unidade_amostral, column: :fk_unidade_amostral, primary_key: :id_unidade

    # --- Tabela Fato: Medição Entomológica ---
    create_table :fato_medicao_entomologica, id: false, comment: 'O coração do DW. Armazena as métricas primárias e se liga ao Evento de Amostragem (ponto de coleta).' do |t|
      t.integer :id_registro, primary_key: true

      # Chaves Estrangeiras
      t.integer :fk_data, null: false
      t.integer :fk_especie, null: false
      t.integer :fk_estudo, null: false
      
      # Adicionado para conectar ao Snowflake (Nível 1) conforme nota "se liga ao Evento de Amostragem"
      t.integer :fk_evento, null: false 

      # Métricas
      t.integer :quantidade, null: false # Abundância
    end

    # FKs da Fato
    add_foreign_key :fato_medicao_entomologica, :dim_tempo, column: :fk_data, primary_key: :id_data
    add_foreign_key :fato_medicao_entomologica, :dim_especie, column: :fk_especie, primary_key: :id_especie
    add_foreign_key :fato_medicao_entomologica, :dim_estudo, column: :fk_estudo, primary_key: :id_estudo
    add_foreign_key :fato_medicao_entomologica, :dim_evento_amostragem, column: :fk_evento, primary_key: :id_evento

    # Índices para performance
    add_index :fato_medicao_entomologica, :fk_data
    add_index :fato_medicao_entomologica, :fk_especie
    add_index :fato_medicao_entomologica, :fk_estudo
    add_index :fato_medicao_entomologica, :fk_evento

    # --- Dimensão Variável Registro (Bridge/Attribute Table) ---
    # Variáveis customizadas (EAV otimizado)
    create_table :dim_variavel_registro, id: false, comment: 'Tabela para variáveis customizadas que variam por registro (e.g., Morfometria, Status Biogeográfico). Otimizada para análises de correlação e IA.' do |t|
      t.integer :id_registro, null: false
      t.integer :id_variavel, null: false
      
      t.string :nome_variavel, limit: 100
      t.decimal :valor_numerico, precision: 18, scale: 4
      t.text :valor_texto
    end

    # FK para a Fato
    add_foreign_key :dim_variavel_registro, :fato_medicao_entomologica, column: :id_registro, primary_key: :id_registro
    
    # Índice para buscas rápidas
    add_index :dim_variavel_registro, :id_registro

    # Definir Chave Primária Composta (Rails não faz isso nativamente no create_table DSL)
    reversible do |dir|
      dir.up { execute "ALTER TABLE dim_variavel_registro ADD PRIMARY KEY (id_registro, id_variavel);" }
    end
  end
end
