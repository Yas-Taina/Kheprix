# frozen_string_literal: true

class CriaTabelaConvites < ActiveRecord::Migration[8.0]
  def change
    create_table :convites do |t|
      t.references :estudo, null: false, foreign_key: true
      t.references :proprietario_envio, null: false, foreign_key: { to_table: :usuarios }
      t.string :email_convidado, null: false
      t.string :token, null: false
      t.datetime :data_expiracao
      t.integer :status, null: false, default: 0

      t.timestamps
    end

    add_index :convites, :token, unique: true
  end
end
