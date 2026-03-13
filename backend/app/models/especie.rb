# frozen_string_literal: true

class Especie < ApplicationRecord
  # 1. Associações
  belongs_to :estudo
  # has_many :registro_ocorrencias, dependent: :destroy  # Descomentar quando RegistroOcorrencia existir

  # 2. Validações (regras de negócio)
  validates :estudo_id, presence: true
  validates :nome_cientifico, uniqueness: { scope: :estudo_id }

  # 3. Scopes
  scope :ordenadas, -> { order(:nome_popular) }
  scope :do_estudo, ->(estudo_id) { where(estudo_id: estudo_id) }
  scope :por_nome_popular, ->(nome) { where("nome_popular ILIKE ?", "%#{nome}%") }
  scope :por_nome_cientifico, ->(nome) { where("nome_cientifico ILIKE ?", "%#{nome}%") }

  # 4. Serialização
  def as_json(options = {})
    super(
      only: %i[
        id
        estudo_id
        foto
        classe
        genero
        nome_popular
        nome_cientifico
        status_conservacao
        nativa_da_regiao
        created_at
      ],
      **options,
    )
  end
end
