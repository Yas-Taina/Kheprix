# frozen_string_literal: true

class Variavel < ApplicationRecord
  self.table_name = "variaveis"

  belongs_to :estudo

  enum :nivel_aplicacao, { campanha: 0, unidade: 1, evento: 2, registro: 3 }
  enum :tipo_dado, { string: 0, number: 1, date: 2 }

  validates :nome, presence: true
  validates :nivel_aplicacao, presence: true
  validates :tipo_dado, presence: true
end
