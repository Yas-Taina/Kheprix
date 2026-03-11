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

ActiveRecord::Schema[8.0].define(version: 2026_03_04_120001) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "pg_catalog.plpgsql"

  create_table "campanhas", force: :cascade do |t|
    t.bigint "estudo_id", null: false
    t.string "nome", limit: 255, null: false
    t.date "data_inicio", null: false
    t.date "data_fim"
    t.text "descricao"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["estudo_id"], name: "index_campanhas_on_estudo_id"
  end

  create_table "colaboradores", id: false, force: :cascade do |t|
    t.integer "estudo_id", null: false
    t.integer "usuario_id", null: false
    t.integer "perfil", default: 0, null: false
    t.index ["estudo_id", "usuario_id"], name: "index_colaboradores_on_estudo_id_and_usuario_id", unique: true
  end

  create_table "estudos", id: :serial, force: :cascade do |t|
    t.string "nome", null: false
    t.text "observacoes"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "usuarios", id: :serial, force: :cascade do |t|
    t.string "nome", null: false
    t.string "email", null: false
    t.string "password_digest", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["email"], name: "index_usuarios_on_email", unique: true
  end

  create_table "valores_variaveis", force: :cascade do |t|
    t.bigint "variavel_id", null: false
    t.integer "id_nivel_aplicacao", null: false
    t.text "valor", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["variavel_id"], name: "index_valores_variaveis_on_variavel_id"
  end

  create_table "variaveis", force: :cascade do |t|
    t.bigint "estudo_id", null: false
    t.string "nome", limit: 100, null: false
    t.string "metrica", limit: 50
    t.integer "nivel_aplicacao", null: false
    t.integer "tipo_dado", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["estudo_id", "nome"], name: "index_variaveis_on_estudo_id_and_nome", unique: true
    t.index ["estudo_id"], name: "index_variaveis_on_estudo_id"
  end

  add_foreign_key "campanhas", "estudos"
  add_foreign_key "colaboradores", "estudos"
  add_foreign_key "colaboradores", "usuarios"
  add_foreign_key "valores_variaveis", "variaveis", column: "variavel_id"
  add_foreign_key "variaveis", "estudos"
end
