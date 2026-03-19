class CriaTabelaEventoAmostragem < ActiveRecord::Migration[8.0]
  def change
    create_table :eventos_amostragem, id: :integer do |t|

      t.datetime :horario_inicio, null: false
      t.datetime :horario_fim, null: false
      t.text :esforco_realizado, null: false

      t.timestamps
    end
  end
end
