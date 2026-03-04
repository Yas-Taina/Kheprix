# frozen_string_literal: true

class CriaTabelaProjeto < ActiveRecord::Migration[8.0]
  def change
    create_table :projetos, id: :integer do |t|
      # Campos do recurso
      t.string :nome, null: false           # NOT NULL — obrigatório no BD
      t.text :descricao                     # Nullable — campo opcional
      t.boolean :ativo, null: false, default: true  # Default no BD

      # created_at e updated_at
      t.timestamps
    end

    # Índice para buscas frequentes
    add_index :projetos, :ativo
  end
end