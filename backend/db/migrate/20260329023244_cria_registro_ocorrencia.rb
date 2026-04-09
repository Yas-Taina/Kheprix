# frozen_string_literal: true

class CriaRegistroOcorrencia < ActiveRecord::Migration[8.0]
  def change
    create_table :registro_ocorrencias, id: :integer do |t|
      t.references :evento_amostragem, null: false, foreign_key: { to_table: :eventos_amostragem }
      t.references :especie, null: false, foreign_key: { to_table: :especies }

      t.date :data, null: false
      t.time :hora, null: false
      t.decimal :latitude, precision: 10, scale: 8, null: false
      t.decimal :longitude, precision: 11, scale: 8, null: false
      t.integer :qtde_individuos
      t.string :foto
      t.boolean :ausencia_especie, default: false
      t.datetime :deleted_at

      t.timestamps
    end

    add_index :registro_ocorrencias, %i[data hora]
    add_index :registro_ocorrencias, :deleted_at
  end
end
