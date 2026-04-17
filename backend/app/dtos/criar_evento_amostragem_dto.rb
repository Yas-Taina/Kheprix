# frozen_string_literal: true

class CriarEventoAmostragemDto
  include ActiveModel::API

  attr_accessor :horario_inicio, :horario_fim, :esforco_real, :valores_variaveis

  validates :horario_inicio, :horario_fim, :esforco_real, presence: true
  validate :valores_variaveis_validos

  def initialize(params = {})
    @horario_inicio = params[:horario_inicio]
    @horario_fim = params[:horario_fim]
    @esforco_real = params[:esforco_real]
    @valores_variaveis = params[:valores_variaveis]
  end

  private

  def valores_variaveis_validos
    return if valores_variaveis.blank?

    unless valores_variaveis.is_a?(Array)
      errors.add(:valores_variaveis, "deve ser um array")
      return
    end

    valores_variaveis.each_with_index do |vv, indice|
      %i[variavel_id valor].each do |campo|
        if vv[campo].blank?
          errors.add(:base, "Valor variável #{indice + 1}: #{campo} não pode ficar em branco")
        end
      end
    end
  end
end
