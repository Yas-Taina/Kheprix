# frozen_string_literal: true

class CriaTabelaUnidadesAmostrais < ActiveRecord::Migration[8.0]
  def change
    create_table :unidades_amostrais do |t|
      t.references :campanha, null: false, foreign_key: true
      t.decimal :latitude, precision: 10, scale: 8, null: false
      t.decimal :longitude, precision: 11, scale: 8, null: false
      t.decimal :raio, precision: 10, scale: 2
      t.text :metodo_coleta
      t.text :esforco_amostral

      t.timestamps
    end
  end
end
