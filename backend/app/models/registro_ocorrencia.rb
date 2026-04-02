# frozen_string_literal: true

class RegistroOcorrencia < ApplicationRecord
  self.table_name = "registro_ocorrencias"

  include SoftDeletavel

  # 1. Associações
  belongs_to :evento_amostragem
  belongs_to :especie

  # 2. Validações
  validates :evento_amostragem_id, presence: true
  validates :especie_id, presence: true
  validates :data, presence: true
  validates :hora, presence: true
  validates :latitude, presence: true
  validates :longitude, presence: true

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
        foto
        ausencia_especie
        created_at
      ],
      **options,
    ).merge("hora" => hora&.strftime("%H:%M:%S"))
  end
end
