# frozen_string_literal: true

class UnidadeAmostral < ApplicationRecord
  include SoftDeletavel

  self.table_name = "unidades_amostrais"

  belongs_to :campanha
  has_many :eventos_amostragem, dependent: :destroy

  validates :nome, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }
  validates :raio, numericality: { greater_than: 0, allow_nil: true }

  scope :recentes, -> { order(updated_at: :desc) }

  def as_json(options = {})
    super(only: %i[id campanha_id nome latitude longitude raio metodo_coleta esforco_amostral created_at updated_at], **options)
  end
end
