# frozen_string_literal: true

class Especie < ApplicationRecord
  include SoftDeletavel

  belongs_to :estudo
  has_many :registro_ocorrencias

  validates :estudo_id, presence: true
  scope :ordenadas, -> { order(:nome_popular) }
  scope :do_estudo, ->(estudo_id) { where(estudo_id: estudo_id) }
  scope :por_nome_popular, ->(nome) { where("nome_popular ILIKE ?", "%#{nome}%") }

  def as_json(options = {})
    super(
      only: %i[
        id
        estudo_id
        classe
        ordem
        familia
        genero
        especie
        nome_popular
        status_conservacao
        endemismo
        created_at
      ],
      **options,
    ).merge(
      "foto" => foto.present? ? "#{ENV.fetch('BACKEND_URL', 'http://localhost:3000')}#{foto}" : nil,
    )
  end
end
