# frozen_string_literal: true

class ValorVariavel < ApplicationRecord
  include SoftDeletavel

  self.table_name = "valores_variaveis"

  belongs_to :variavel

  validates :id_nivel_aplicacao, presence: true
  validates :valor, presence: true
end
