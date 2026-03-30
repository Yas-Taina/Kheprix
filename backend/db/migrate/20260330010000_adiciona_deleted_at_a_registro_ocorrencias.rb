# frozen_string_literal: true

class AdicionaDeletedAtARegistroOcorrencias < ActiveRecord::Migration[8.0]
  def change
    add_column :registro_ocorrencias, :deleted_at, :datetime
    add_index :registro_ocorrencias, :deleted_at
  end
end
