# frozen_string_literal: true

class CadastrarUnidadeAmostralDto
  include ActiveModel::API

  attr_accessor :nome, :latitude, :longitude, :raio, :metodo_coleta, :esforco_amostral, :valores_variaveis

  validates :nome, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }
  validate :valores_variaveis_validos

  def initialize(params = {})
    @nome = params[:nome]
    @latitude = params[:latitude]
    @longitude = params[:longitude]
    @raio = params[:raio]
    @metodo_coleta = params[:metodo_coleta]
    @esforco_amostral = params[:esforco_amostral]
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
