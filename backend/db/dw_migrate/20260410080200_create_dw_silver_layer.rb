class CreateDwSilverLayer < ActiveRecord::Migration[8.0]
  def change
    # =========================================================================
    # CAMADA SILVER: Entidades limpas — 1:1 com staging, sem JOINs entre fontes
    # Responsabilidade: filtrar soft deletes, padronizar nomes e decodificar
    # valores brutos (enums, tipos). Nenhuma regra de negócio aplicada aqui.
    # =========================================================================

    # --- Silver: Estudos ---
    create_table :silver_estudos, id: false do |t|
      t.integer   :id_estudo, primary_key: true
      t.string    :nome_estudo, limit: 255
      t.timestamp :updated_at
    end

    # --- Silver: Campanhas ---
    create_table :silver_campanhas, id: false do |t|
      t.integer   :id_campanha, primary_key: true
      t.integer   :fk_estudo_src, null: false  # FK bruta, validada na Gold
      t.string    :nome_campanha, limit: 255
      t.date      :data_inicio
      t.timestamp :data_atualizacao
      t.text      :descricao
    end

    # --- Silver: Unidades Amostrais ---
    create_table :silver_unidades_amostrais, id: false do |t|
      t.integer   :id_unidade, primary_key: true
      t.integer   :fk_campanha_src, null: false  # FK bruta, validada na Gold
      t.decimal   :latitude,  precision: 10, scale: 8
      t.decimal   :longitude, precision: 11, scale: 8
      t.decimal   :raio,      precision: 10, scale: 2
      t.text      :metodo_coleta
      t.text      :esforco_amostral
    end

    # --- Silver: Eventos de Amostragem ---
    create_table :silver_eventos_amostragem, id: false do |t|
      t.integer   :id_evento, primary_key: true
      t.integer   :fk_unidade_src, null: false  # FK bruta, validada na Gold
      t.timestamp :horario_inicio
      t.timestamp :data_atualizacao
      t.text      :esforco_real
    end

    # --- Silver: Registro de Ocorrências (dados brutos — regra de ausencia_especie aplicada na Gold) ---
    create_table :silver_registro_ocorrencias, id: false do |t|
      t.integer   :id_registro, primary_key: true
      t.integer   :especie_id
      t.integer   :evento_amostragem_id
      t.date      :data
      t.decimal   :latitude,  precision: 10, scale: 8
      t.decimal   :longitude, precision: 11, scale: 8
      t.boolean   :ausencia_especie
      t.integer   :qtde_individuos
      t.timestamp :data_atualizacao
    end

    # --- Silver: Espécies (com nome_cientifico derivado da padronização taxonômica) ---
    create_table :silver_especies, id: false do |t|
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

    # --- Silver: Variáveis (com enums nivel_aplicacao e tipo_dado decodificados) ---
    create_table :silver_variaveis, id: false do |t|
      t.integer   :id, primary_key: true
      t.string    :nome,             limit: 255
      t.string    :metrica,          limit: 100
      t.string    :nivel_aplicacao,  limit: 50
      t.string    :tipo_dado,        limit: 50
      t.timestamp :created_at
      t.timestamp :updated_at
    end

    # --- Silver: Valores de Variáveis com tipos resolvidos ---
    create_table :silver_valores_variaveis, id: false do |t|
      t.integer   :id, primary_key: true
      t.integer   :variavel_id,          null: false
      t.integer   :id_nivel_aplicacao,   null: false
      t.decimal   :valor_numerico, precision: 18, scale: 4
      t.text      :valor_texto
      t.date      :valor_data
      t.timestamp :updated_at
    end
    add_foreign_key :silver_valores_variaveis, :silver_variaveis, column: :variavel_id, primary_key: :id

    # --- Silver: Variáveis Agregadas por entidade e nível (helper para variaveis_customizadas) ---
    create_table :silver_variaveis_agregadas, id: false do |t|
      t.integer :id_nivel_aplicacao, null: false
      t.string  :nivel_aplicacao,    null: false, limit: 50
      t.jsonb   :variaveis_customizadas, default: {}
    end
    add_index :silver_variaveis_agregadas, [:id_nivel_aplicacao, :nivel_aplicacao],
              unique: true, name: 'idx_silver_vars_agregadas_pk'

    # =========================================================================
    # ÍNDICES DE PERFORMANCE — Colunas usadas como JOIN keys nas queries Gold
    # Sem estes índices, cada dimensão Gold faz seq scan completo nas Silver.
    # =========================================================================
    add_index :silver_campanhas,            :fk_estudo_src,        name: 'idx_silver_campanhas_estudo'
    add_index :silver_unidades_amostrais,   :fk_campanha_src,      name: 'idx_silver_unidades_campanha'
    add_index :silver_eventos_amostragem,   :fk_unidade_src,       name: 'idx_silver_eventos_unidade'
    add_index :silver_registro_ocorrencias, :evento_amostragem_id, name: 'idx_silver_registros_evento'
    add_index :silver_valores_variaveis,    :variavel_id,          name: 'idx_silver_valores_variavel'
    add_index :silver_valores_variaveis,    :id_nivel_aplicacao,   name: 'idx_silver_valores_nivel'
  end
end
