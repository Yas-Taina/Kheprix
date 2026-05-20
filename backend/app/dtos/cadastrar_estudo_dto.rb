# frozen_string_literal: true

class CadastrarEstudoDto
  include ActiveModel::API

  attr_accessor :nome, :observacoes, :variaveis

  NIVEIS_APLICACAO = %w[campanha unidade evento registro].freeze
  TIPOS_DADO = %w[string number date boolean].freeze

  validates :nome, presence: true, length: { maximum: 120 }
  validates :observacoes, length: { maximum: 1000 }, allow_blank: true
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

      if variavel[:nivel_aplicacao].present? && !NIVEIS_APLICACAO.include?(variavel[:nivel_aplicacao].to_s)
        errors.add(:base, "Variável #{indice + 1}: o nível precisa ser campanha, unidade, evento ou registro.")
      end

      if variavel[:tipo_dado].present? && !TIPOS_DADO.include?(variavel[:tipo_dado].to_s)
        errors.add(:base, "Variável #{indice + 1}: o tipo precisa ser texto, número, data ou verdadeiro/falso.")
      end
    end
  end
end
