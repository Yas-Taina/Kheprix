# frozen_string_literal: true

class CriarRegistroOcorrenciaDto
  include ActiveModel::API

  attr_accessor :especie_id, :data, :hora, :latitude, :longitude, :qtde_individuos, :foto, :ausencia_especie,
                :valores_variaveis

  validates :especie_id, presence: true
  validates :data, presence: true
  validates :hora, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }
  validate :valores_variaveis_validos

  def initialize(params = {})
    @especie_id = params[:especie_id]
    @data = params[:data]
    @hora = params[:hora]
    @latitude = params[:latitude]
    @longitude = params[:longitude]
    @qtde_individuos = params[:qtde_individuos]
    @foto = params[:foto]
    @ausencia_especie = params[:ausencia_especie]
    @valores_variaveis = params[:valores_variaveis]
  end

  def atributos
    {
      especie_id:,
      data:,
      hora:,
      latitude:,
      longitude:,
      qtde_individuos:,
      foto:,
      ausencia_especie:
    }.compact
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
