# frozen_string_literal: true

class CriaTabelaEspecie < ActiveRecord::Migration[8.0]
  def change
    create_table :especies, id: :integer do |t|
      t.integer :estudo_id, null: false

      t.string :foto                                          # URL da foto (opcional)
      t.string :classe, limit: 100                            # Classe taxonômica (opcional)
      t.string :ordem, limit: 100                             # Ordem taxonômica (opcional)
      t.string :familia, limit: 100                           # Família taxonômica (opcional)
      t.string :genero, limit: 100                            # Gênero taxonômico (opcional)
      t.string :especie, limit: 100                            # Espécie taxonômica (opcional)
      t.string :nome_popular                                  # Nome popular (opcional)
      t.string :status_conservacao, limit: 100                # Ex.: "Em perigo", "Vulnerável" (opcional)
      t.boolean :endemismo, default: false                    # Endêmica da região?

      t.timestamps
    end

    add_index :especies, %i[estudo_id especie], unique: true
    add_foreign_key :especies, :estudos
  end
end
