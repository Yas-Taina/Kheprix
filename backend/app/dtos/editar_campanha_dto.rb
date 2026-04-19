# frozen_string_literal: true

class EditarCampanhaDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :nome, :data_inicio, :data_fim, :descricao, :valores_variaveis

  CAMPOS_OBRIGATORIOS = %i[nome data_inicio descricao].freeze

  validates :nome, presence: true
  validates :data_inicio, presence: true
  validate :campos_informados
  validate :valida_valores_variaveis_edicao

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
end
