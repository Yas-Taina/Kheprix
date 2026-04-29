# frozen_string_literal: true

class EventoAmostragem < ApplicationRecord
  include SoftDeletavel

  belongs_to :unidade_amostral
  has_many :registro_ocorrencias
  has_many :valores_variaveis,
    -> { joins(:variavel).where(variaveis: { nivel_aplicacao: :evento }) },
    foreign_key: :id_nivel_aplicacao,
    dependent: :destroy

  validates :horario_inicio, :esforco_real, presence: true

  scope :ordenados, -> { order(horario_inicio: :desc) }

  def as_json(options = {})
    super(
      only: %i[
        id
        unidade_amostral_id
        horario_inicio
        esforco_real
        created_at
        updated_at
      ],
      **options,
    ).merge(
      "valores_variaveis" => valores_variaveis.map { |vv|
        { "id" => vv.id, "variavel_id" => vv.variavel_id, "valor" => vv.valor }
      },
    )
  end
end
