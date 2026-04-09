# frozen_string_literal: true

class CriaTabelaCampanhas < ActiveRecord::Migration[8.0]
  def change
    create_table :campanhas do |t|
      t.references :estudo, null: false, foreign_key: true
      t.string :nome, limit: 255, null: false
      t.date :data_inicio, null: false
      t.date :data_fim
      t.text :descricao

      t.timestamps
    end
  end
end
