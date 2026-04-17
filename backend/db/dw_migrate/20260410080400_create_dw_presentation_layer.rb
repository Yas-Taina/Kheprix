class CreateDwPresentationLayer < ActiveRecord::Migration[8.0]
  def change
    # =========================================================================
    # CAMADA GOLD: PRESENTATION LAYER
    # Tabelas desnormalizadas pré-computadas para consumo direto pela API.
    # Alternativa às views: elimina re-execução de JOINs a cada requisição,
    # essencial para volumetria grande. Atualizadas via UPSERT a cada pipeline.
    # =========================================================================

    create_table :indicadores_dashboard, id: false do |t|
      t.integer   :id_registro,        primary_key: true
      t.integer   :fk_estudo,          null: false
      t.string    :nome_estudo,        limit: 255
      t.string    :nome_campanha,      limit: 255
      t.date      :data_inicio_campanha
      t.decimal   :latitude,           precision: 10, scale: 8
      t.decimal   :longitude,          precision: 11, scale: 8
      t.date      :data_registro
      t.integer   :ano
      t.integer   :mes
      t.string    :estacao,            limit: 50
      t.string    :nome_cientifico,    limit: 255
      t.string    :nome_popular,       limit: 255
      t.string    :ordem,              limit: 100
      t.string    :familia,            limit: 100
      t.string    :genero,             limit: 100
      t.string    :status_conservacao, limit: 100
      t.integer   :quantidade
      t.boolean   :is_endemica,        default: false
      t.boolean   :is_ameacada,        default: false
    end
    add_index :indicadores_dashboard, :fk_estudo, name: 'idx_presentation_indicadores_estudo'

    create_table :analises_estatisticas, id: false do |t|
      t.integer   :id_registro,          null: false
      t.integer   :id_variavel,          null: false, default: 0  # 0 = registro sem variável
      t.integer   :id_estudo,            null: false
      t.string    :nome_estudo,          limit: 255
      t.integer   :fk_campanha
      t.string    :nome_campanha,        limit: 255
      t.integer   :fk_unidade_amostral
      t.string    :nome_unidade_amostral, limit: 255
      t.integer   :fk_evento
      t.date      :data_registro
      t.integer   :ano
      t.integer   :mes
      t.string    :especie,              limit: 255
      t.string    :ordem,                limit: 100
      t.string    :familia,              limit: 100
      t.integer   :abundancia
      t.string    :nivel_variavel,       limit: 50
      t.string    :nome_variavel,        limit: 255
      t.decimal   :valor_numerico,       precision: 18, scale: 4
      t.text      :valor_texto
      t.date      :valor_data
    end
    add_index :analises_estatisticas, [:id_registro, :id_variavel], unique: true, name: 'idx_presentation_analises_unique'
    add_index :analises_estatisticas, :id_estudo, name: 'idx_presentation_analises_estudo'
  end
end
