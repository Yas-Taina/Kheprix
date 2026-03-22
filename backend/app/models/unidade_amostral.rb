# frozen_string_literal: true

class UnidadeAmostral < ApplicationRecord
  self.table_name = "unidades_amostrais"

  belongs_to :campanha

  validates :nome, presence: true
  validates :latitude, presence: true
  validates :longitude, presence: true

  scope :recentes, -> { order(updated_at: :desc) }

  def as_json(options = {})
    super(only: %i[id campanha_id nome latitude longitude raio metodo_coleta esforco_amostral created_at updated_at], **options)
  end
end
