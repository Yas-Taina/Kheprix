# frozen_string_literal: true

class AdicionaNomeAUnidadesAmostrais < ActiveRecord::Migration[8.0]
  def change
    add_column :unidades_amostrais, :nome, :string, null: false
  end
end
