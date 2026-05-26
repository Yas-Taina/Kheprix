class CreateDwStagingLayer < ActiveRecord::Migration[8.0]
  def up
    execute "CREATE SCHEMA IF NOT EXISTS staging;"

    create_table "staging.usuarios", id: :integer do |t|
      t.string :nome
      t.string :email
      t.string :password_digest
      t.bigint :ultimo_estudo_acessado_id
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.estudos", id: :integer do |t|
      t.string :nome
      t.text :observacoes
      t.string :codigo
      t.string :senha_autocadastro
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.colaboradores", id: false do |t|
      t.integer :estudo_id
      t.integer :usuario_id
      t.integer :perfil
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
    end

    create_table "staging.campanhas", id: :integer do |t|
      t.integer :estudo_id
      t.string :nome
      t.date :data_inicio
      t.date :data_fim
      t.text :descricao
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.especies", id: :integer do |t|
      t.integer :estudo_id
      t.string :foto
      t.string :classe
      t.string :ordem
      t.string :familia
      t.string :genero
      t.string :especie
      t.string :nome_popular
      t.string :status_conservacao
      t.boolean :endemismo, default: false
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.unidades_amostrais", id: :integer do |t|
      t.integer :campanha_id
      t.decimal :latitude
      t.decimal :longitude
      t.decimal :raio
      t.text :metodo_coleta
      t.text :esforco_amostral
      t.string :nome
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.eventos_amostragem", id: :integer do |t|
      t.integer :unidade_amostral_id
      t.datetime :horario_inicio
      t.datetime :horario_fim
      t.text :esforco_real
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.registro_ocorrencias", id: :integer do |t|
      t.integer :evento_amostragem_id
      t.integer :especie_id
      t.date :data
      t.time :hora
      t.decimal :latitude
      t.decimal :longitude
      t.integer :qtde_individuos
      t.string :foto
      t.boolean :ausencia_especie
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.variaveis", id: :integer do |t|
      t.integer :estudo_id
      t.string :nome
      t.string :metrica
      t.integer :nivel_aplicacao
      t.integer :tipo_dado
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end

    create_table "staging.valores_variaveis", id: :integer do |t|
      t.integer :variavel_id
      t.integer :id_nivel_aplicacao
      t.text :valor
      t.datetime :deleted_at
      t.datetime :loaded_at, default: -> { 'CURRENT_TIMESTAMP' }
      t.timestamps
    end
  end

  def down
    execute "DROP SCHEMA IF EXISTS staging CASCADE;"
  end
end
