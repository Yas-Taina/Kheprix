# frozen_string_literal: true

class AdicionaDeletedAtParaSoftDelete < ActiveRecord::Migration[8.0]
  def change
    add_column :estudos, :deleted_at, :datetime
    add_column :campanhas, :deleted_at, :datetime
    add_column :especies, :deleted_at, :datetime
    add_column :unidades_amostrais, :deleted_at, :datetime
    add_column :eventos_amostragem, :deleted_at, :datetime
    add_column :variaveis, :deleted_at, :datetime
    add_column :valores_variaveis, :deleted_at, :datetime

    add_index :estudos, :deleted_at
    add_index :campanhas, :deleted_at
    add_index :especies, :deleted_at
    add_index :unidades_amostrais, :deleted_at
    add_index :eventos_amostragem, :deleted_at
    add_index :variaveis, :deleted_at
    add_index :valores_variaveis, :deleted_at
  end
end
