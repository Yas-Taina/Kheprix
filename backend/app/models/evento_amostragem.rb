class EventoAmostragem < ApplicationRecord
  validates :horario_inicio, presence: true
  validates :horario_fim_apos_inicio, presence: true

  scope :ordenados, -> { order(horario_inicio: :desc) }

  def as_json(options = {})
    super(
        only: %i[
          id
          horario_inicio
          horario_fim
          esforco_real
          created_at
        ],
        **options,
    )
  end

    private

  def horario_fim_apos_inicio
    return if horario_fim.blank? || horario_inicio.blan?

    if horario_fim <= horario_inicio
        errors.add(:horario_fim, "deve ser posterior ao horário de início")
    end
  end
end
