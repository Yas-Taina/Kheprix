# frozen_string_literal: true

class AjustaConstraintsUnicasParaSoftDelete < ActiveRecord::Migration[8.0]
  def change
    remove_index :estudos, :codigo
    add_index :estudos, :codigo, unique: true, where: "deleted_at IS NULL"

    remove_index :variaveis, [ :estudo_id, :nome ]
    add_index :variaveis, [ :estudo_id, :nome ], unique: true, where: "deleted_at IS NULL"
  end
end
