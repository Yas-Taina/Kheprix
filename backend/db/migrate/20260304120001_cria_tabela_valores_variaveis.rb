# frozen_string_literal: true

class CriaTabelaValoresVariaveis < ActiveRecord::Migration[8.0]
  def change
    create_table :valores_variaveis do |t|
      t.references :variavel, null: false, foreign_key: { to_table: :variaveis }
      t.integer :id_nivel_aplicacao, null: false
      t.text :valor, null: false

      t.timestamps
    end
  end
end
