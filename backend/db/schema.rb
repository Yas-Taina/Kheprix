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

ActiveRecord::Schema[8.0].define(version: 2026_03_02_002310) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "pg_catalog.plpgsql"

  create_table "especies", id: :serial, force: :cascade do |t|
    t.string "foto"
    t.string "classe", limit: 100
    t.string "genero", limit: 100
    t.string "nome_popular"
    t.string "nome_cientifico"
    t.string "status_conservacao", limit: 100
    t.boolean "nativa_da_regiao", default: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "projetos", id: :serial, force: :cascade do |t|
    t.string "nome", null: false
    t.text "descricao"
    t.boolean "ativo", default: true, null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["ativo"], name: "index_projetos_on_ativo"
  end

  create_table "usuarios", id: :serial, force: :cascade do |t|
    t.string "nome", null: false
    t.string "email", null: false
    t.string "password_digest", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["email"], name: "index_usuarios_on_email", unique: true
  end
end
