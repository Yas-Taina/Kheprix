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

  MAX_VALORES_VARIAVEIS = 200

  def valores_variaveis_validos
    return if valores_variaveis.blank?

    unless valores_variaveis.is_a?(Array)
      errors.add(:valores_variaveis, "deve ser um array")
      return
    end

    if valores_variaveis.length > MAX_VALORES_VARIAVEIS
      errors.add(:valores_variaveis, "excede o limite de #{MAX_VALORES_VARIAVEIS} itens")
      return
    end

    unless valores_variaveis.all? { |vv| vv.is_a?(Hash) || vv.is_a?(ActionController::Parameters) }
      errors.add(:valores_variaveis, "cada item deve ser um objeto")
      return
    end

    var_ids = valores_variaveis.filter_map { |vv| vv[:variavel_id] }
    if var_ids.length != var_ids.uniq.length
      errors.add(:valores_variaveis, "contém variavel_id duplicados")
    end

    valores_variaveis.each_with_index do |vv, indice|
      if vv[:id].present?
        errors.add(:base, "Valor variável #{indice + 1}: id não permitido em criação (use PATCH para atualizar)")
      end
      %i[variavel_id valor].each do |campo|
        if vv[campo].blank?
          errors.add(:base, "Valor variável #{indice + 1}: #{campo} não pode ficar em branco")
        end
      end
    end
  end
end
