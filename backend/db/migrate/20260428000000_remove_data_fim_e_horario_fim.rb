# frozen_string_literal: true

class RemoveDataFimEHorarioFim < ActiveRecord::Migration[8.0]
  def change
    remove_column :campanhas, :data_fim, :date, if_exists: true
    remove_column :eventos_amostragem, :horario_fim, :datetime, null: false, if_exists: true
  end
end
