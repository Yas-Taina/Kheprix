# frozen_string_literal: true

class CriaTabelaEspecie < ActiveRecord::Migration[8.0]
  def change
    create_table :especies, id: :integer do |t|
      # FK — descomentar quando a tabela estudos existir
      # t.integer :estudo_id, null: false

      t.string :foto                                          # URL da foto (opcional)
      t.string :classe, limit: 100                            # Classe taxonômica (opcional)
      t.string :genero, limit: 100                            # Gênero taxonômico (opcional)
      t.string :nome_popular                                  # Nome popular (opcional)
      t.string :nome_cientifico                               # Nome científico (opcional)
      t.string :status_conservacao, limit: 100                # Ex.: "Em perigo", "Vulnerável" (opcional)
      t.boolean :nativa_da_regiao, default: false             # Default false

      t.timestamps
    end

    # Índice composto único — descomentar quando a tabela estudos existir
    # add_index :especies, %i[estudo_id nome_cientifico], unique: true

    # FK — descomentar quando a tabela estudos existir
    # add_foreign_key :especies, :estudos
  end
end