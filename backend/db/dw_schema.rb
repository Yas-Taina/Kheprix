# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `bin/rails
# db:schema:load`. When creating a new database, `bin/rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema[8.0].define(version: 2026_03_23_090000) do
  create_schema "staging"

  # These are extensions that must be enabled in order to support this database
  enable_extension "pg_catalog.plpgsql"

  create_table "dim_campanha", primary_key: "id_campanha", id: :serial, comment: "Contexto geral da campanha. Desnormalizada na hierarquia.", force: :cascade do |t|
    t.integer "fk_estudo", null: false
    t.date "data_inicio"
    t.decimal "variavel_a", precision: 10, scale: 2
    t.string "variavel_b", limit: 100
  end

  create_table "dim_especie", primary_key: "id_especie", id: :serial, comment: "Contexto Biológico da Espécie.", force: :cascade do |t|
    t.string "nome_cientifico", limit: 255
    t.string "nome_popular", limit: 255
    t.string "classe", limit: 100
    t.string "status_conservacao", limit: 100
  end

  create_table "dim_estudo", primary_key: "id_estudo", id: :serial, comment: "Inclui variáveis de estudo customizadas (e.g., Altitude, Tipo de Solo).", force: :cascade do |t|
    t.string "nome_estudo", limit: 255
    t.decimal "variavel_a", precision: 10, scale: 2
    t.string "variavel_b", limit: 100
  end

  create_table "dim_evento_amostragem", primary_key: "id_evento", id: :serial, comment: "Nível mais baixo da hierarquia de coleta. Inclui variáveis instantâneas.", force: :cascade do |t|
    t.integer "fk_unidade_amostral", null: false
    t.datetime "horario_inicio", precision: nil
    t.decimal "variavel_a", precision: 10, scale: 2
    t.string "variavel_b", limit: 100
  end

  create_table "dim_tempo", primary_key: "id_data", id: :serial, comment: "Dimensão Temporal, otimizada para análise de sazonalidade.", force: :cascade do |t|
    t.date "data_completa"
    t.integer "dia"
    t.integer "mes"
    t.integer "ano"
    t.integer "trimestre"
    t.string "estacao", limit: 50
  end

  create_table "dim_unidade_amostral", primary_key: "id_unidade", id: :serial, comment: "Contexto espacial e metodológico. Inclui variáveis de unidade.", force: :cascade do |t|
    t.integer "fk_campanha", null: false
    t.decimal "latitude", precision: 10, scale: 8
    t.decimal "longitude", precision: 11, scale: 8
    t.text "metodo_coleta"
    t.decimal "variavel_a", precision: 10, scale: 2
    t.string "variavel_b", limit: 100
  end

  create_table "dim_variavel_registro", primary_key: ["id_registro", "id_variavel"], comment: "Tabela para variáveis customizadas que variam por registro (e.g., Morfometria, Status Biogeográfico). Otimizada para análises de correlação e IA.", force: :cascade do |t|
    t.integer "id_registro", null: false
    t.integer "id_variavel", null: false
    t.string "nome_variavel", limit: 100
    t.decimal "valor_numerico", precision: 18, scale: 4
    t.text "valor_texto"
    t.index ["id_registro"], name: "index_dim_variavel_registro_on_id_registro"
  end

  create_table "fato_medicao_entomologica", primary_key: "id_registro", id: :serial, comment: "O coração do DW. Armazena as métricas primárias e se liga ao Evento de Amostragem (ponto de coleta).", force: :cascade do |t|
    t.integer "fk_data", null: false
    t.integer "fk_especie", null: false
    t.integer "fk_estudo", null: false
    t.integer "fk_evento", null: false
    t.integer "quantidade", null: false
    t.index ["fk_data"], name: "index_fato_medicao_entomologica_on_fk_data"
    t.index ["fk_especie"], name: "index_fato_medicao_entomologica_on_fk_especie"
    t.index ["fk_estudo"], name: "index_fato_medicao_entomologica_on_fk_estudo"
    t.index ["fk_evento"], name: "index_fato_medicao_entomologica_on_fk_evento"
  end

  add_foreign_key "dim_campanha", "dim_estudo", column: "fk_estudo", primary_key: "id_estudo"
  add_foreign_key "dim_evento_amostragem", "dim_unidade_amostral", column: "fk_unidade_amostral", primary_key: "id_unidade"
  add_foreign_key "dim_unidade_amostral", "dim_campanha", column: "fk_campanha", primary_key: "id_campanha"
  add_foreign_key "dim_variavel_registro", "fato_medicao_entomologica", column: "id_registro", primary_key: "id_registro"
  add_foreign_key "fato_medicao_entomologica", "dim_especie", column: "fk_especie", primary_key: "id_especie"
  add_foreign_key "fato_medicao_entomologica", "dim_estudo", column: "fk_estudo", primary_key: "id_estudo"
  add_foreign_key "fato_medicao_entomologica", "dim_evento_amostragem", column: "fk_evento", primary_key: "id_evento"
  add_foreign_key "fato_medicao_entomologica", "dim_tempo", column: "fk_data", primary_key: "id_data"
end
