# frozen_string_literal: true

class CriaTabelaVariavel < ActiveRecord::Migration[8.0]
  def change
    create_table :variaveis do |t|
      t.references :estudo, null: false, foreign_key: true
      t.string :nome, limit: 100, null: false
      t.string :metrica, limit: 50
      t.integer :nivel_aplicacao, null: false
      t.integer :tipo_dado, null: false

      t.timestamps
    end

    add_index :variaveis, %i[estudo_id nome], unique: true
  end
end
