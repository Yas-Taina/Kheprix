# frozen_string_literal: true

class Especie < ApplicationRecord
  # 1. Associações
  # belongs_to :estudo                                    # Descomentar quando Estudo existir
  # has_many :registro_ocorrencias, dependent: :restrict_with_error  # Descomentar quando RegistroOcorrencia existir

  # 2. Validações (regras de negócio)
  # validates :estudo_id, presence: true                  # Descomentar quando Estudo existir
  # validates :nome_cientifico, uniqueness: { scope: :estudo_id }  # Descomentar quando Estudo existir

  # 3. Scopes
  scope :ordenadas, -> { order(:nome_popular) }
  # scope :do_estudo, ->(estudo_id) { where(estudo_id: estudo_id) }  # Descomentar quando Estudo existir

  # 4. Serialização
  def as_json(options = {})
    super(
      only: %i[
        id
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