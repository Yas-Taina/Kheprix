# frozen_string_literal: true

class Campanha < ApplicationRecord
  belongs_to :estudo
  has_many :valores_variaveis, foreign_key: :id_nivel_aplicacao, dependent: :destroy

  validates :nome, presence: true
  validates :data_inicio, presence: true

  def as_json(options = {})
    super(only: %i[id nome data_inicio data_fim descricao created_at updated_at], **options)
  end
end
