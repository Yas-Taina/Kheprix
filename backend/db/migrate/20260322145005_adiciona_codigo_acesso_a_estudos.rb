# frozen_string_literal: true

class AdicionaCodigoAcessoAEstudos < ActiveRecord::Migration[8.0]
  def change
    add_column :estudos, :codigo, :string
    add_column :estudos, :senha_autocadastro, :string
    add_index :estudos, :codigo, unique: true
  end
end
