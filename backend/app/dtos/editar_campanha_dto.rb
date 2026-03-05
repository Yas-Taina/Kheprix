# frozen_string_literal: true

class EditarCampanhaDto
  include ActiveModel::API

  attr_accessor :nome, :data_inicio, :data_fim, :descricao, :valores_variaveis

  validates :nome, presence: true
  validates :data_inicio, presence: true
  validate :valores_variaveis_validos

  def initialize(params = {})
    @nome = params[:nome]
    @data_inicio = params[:data_inicio]
    @data_fim = params[:data_fim]
    @descricao = params[:descricao]
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
