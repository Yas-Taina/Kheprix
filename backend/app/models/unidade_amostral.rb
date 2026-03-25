# frozen_string_literal: true

class UnidadeAmostral < ApplicationRecord
  self.table_name = "unidades_amostrais"

  belongs_to :campanha
  has_many :eventos_amostragem, dependent: :destroy

  validates :nome, presence: true
  validates :latitude, presence: true
  validates :longitude, presence: true
  validates :raio, numericality: { greater_than: 0, allow_nil: true }

  scope :recentes, -> { order(updated_at: :desc) }

  def as_json(options = {})
    super(only: %i[id campanha_id nome latitude longitude raio metodo_coleta esforco_amostral created_at updated_at], **options)
  end
end
