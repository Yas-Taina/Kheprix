# frozen_string_literal: true

class CriaTabelaUsuarios < ActiveRecord::Migration[8.0]
  def change
    create_table :usuarios, id: :integer do |t|
      t.string :nome, null: false
      t.string :email, null: false
      t.string :password_digest, null: false

      t.timestamps
    end

    add_index :usuarios, :email, unique: true
  end
end
