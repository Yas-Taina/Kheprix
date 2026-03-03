# frozen_string_literal: true

class CadastrarEstudoDto
  include ActiveModel::API

  attr_accessor :nome, :observacoes, :variaveis

  validates :nome, presence: true
  validates :variaveis, presence: true
  validate :variaveis_devem_ser_array
  validate :validar_campos_variaveis

  def initialize(params = {})
    @nome = params[:nome]
    @observacoes = params[:observacoes]
    @variaveis = params[:variaveis]
  end

  private

  def variaveis_devem_ser_array
    return if variaveis.blank?

    unless variaveis.is_a?(Array) && variaveis.any?
      errors.add(:variaveis, "deve ser um array não vazio")
    end
  end

  def validar_campos_variaveis
    return unless variaveis.is_a?(Array)

    variaveis.each_with_index do |variavel, indice|
      %i[nome nivel_aplicacao tipo_dado].each do |campo|
        if variavel[campo].blank?
          errors.add(:base, "Variável #{indice + 1}: #{campo} não pode ficar em branco")
        end
      end
    end
  end
end
