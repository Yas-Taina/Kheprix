# frozen_string_literal: true

class CriaTabelasEstudoEColaborador < ActiveRecord::Migration[8.0]
  def change
    create_table :estudos, id: :integer do |t|
      t.string :nome, null: false
      t.text :observacoes

      t.timestamps
    end

    create_table :colaboradores, id: false do |t|
      t.integer :estudo_id, null: false
      t.integer :usuario_id, null: false
      t.integer :perfil, null: false, default: 0
    end

    add_index :colaboradores, %i[estudo_id usuario_id], unique: true
    add_foreign_key :colaboradores, :estudos
    add_foreign_key :colaboradores, :usuarios
  end
end
