# frozen_string_literal: true

class CriaTabelaEventoAmostragem < ActiveRecord::Migration[8.0]
  def change
    create_table :eventos_amostragem, id: :integer do |t|
      t.references :unidade_amostral, null: false, foreign_key: { to_table: :unidades_amostrais }

      t.datetime :horario_inicio, null: false
      t.text :esforco_real, null: false

      t.timestamps
    end
  end
end
