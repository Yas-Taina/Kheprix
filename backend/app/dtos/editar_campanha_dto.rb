# frozen_string_literal: true

class EditarCampanhaDto
  include ActiveModel::API

  attr_accessor :nome, :data_inicio, :data_fim, :descricao, :valores_variaveis

  CAMPOS_OBRIGATORIOS = %i[nome data_inicio descricao].freeze

  validates :nome, presence: true
  validates :data_inicio, presence: true
  validate :campos_informados
  validate :valores_variaveis_validos

  def initialize(params = {})
    @chaves_informadas = params.respond_to?(:keys) ? params.keys.map(&:to_sym) : []
    @nome = params[:nome]
    @data_inicio = params[:data_inicio]
    @data_fim = params[:data_fim]
    @descricao = params[:descricao]
    @valores_variaveis = params[:valores_variaveis]
  end

  private

  def campos_informados
    CAMPOS_OBRIGATORIOS.each do |campo|
      unless @chaves_informadas.include?(campo)
        errors.add(campo, "deve ser informado")
      end
    end
  end

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

    ids = valores_variaveis.filter_map { |vv| vv[:id] }
    if ids.length != ids.uniq.length
      errors.add(:valores_variaveis, "contém ids duplicados")
    end

    valores_variaveis.each_with_index do |vv, indice|
      if vv[:id].blank?
        errors.add(:base, "Valor variável #{indice + 1}: id é obrigatório em atualização")
      elsif !vv[:id].is_a?(Integer) || vv[:id] <= 0
        errors.add(:base, "Valor variável #{indice + 1}: id deve ser inteiro positivo")
      end
      if vv[:valor].blank?
        errors.add(:base, "Valor variável #{indice + 1}: valor não pode ficar em branco")
      end
    end
  end
end
