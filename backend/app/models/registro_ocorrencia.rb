# frozen_string_literal: true

class RegistroOcorrencia < ApplicationRecord
  self.table_name = "registro_ocorrencias"

  include SoftDeletavel

  # 1. Associações
  belongs_to :evento_amostragem
  belongs_to :especie
  has_many :valores_variaveis, foreign_key: :id_nivel_aplicacao, dependent: :destroy

  # 2. Validações
  validates :evento_amostragem_id, presence: true
  validates :especie_id, presence: true
  validates :data, presence: true
  validates :hora, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }

  # 3. Scopes
  scope :do_evento, ->(evento_id) { where(evento_amostragem_id: evento_id) }
  scope :por_data, -> { order(data: :desc, hora: :desc) }
  scope :por_especie, ->(especie_id) { where(especie_id: especie_id) }

  # 4. Serialização
  def as_json(options = {})
    super(
      only: %i[
        id
        evento_amostragem_id
        especie_id
        data
        latitude
        longitude
        qtde_individuos
        ausencia_especie
        created_at
      ],
      **options,
    ).merge(
      "hora" => hora&.strftime("%H:%M:%S"),
      "foto" => foto.present? ? "#{ENV.fetch('BACKEND_URL', 'http://localhost:3000')}#{foto}" : nil
    )
  end
end
