# frozen_string_literal: true

class RemoveDataFimEHorarioFimDeStaging < ActiveRecord::Migration[8.0]
  def change
    remove_column "staging.campanhas", :data_fim, :date, if_exists: true
    remove_column "staging.eventos_amostragem", :horario_fim, :datetime, if_exists: true
  end
end
