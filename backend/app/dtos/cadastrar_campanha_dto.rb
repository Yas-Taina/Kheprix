# frozen_string_literal: true

class CadastrarCampanhaDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :nome, :data_inicio, :descricao, :valores_variaveis

  validates :nome, presence: true
  validates :data_inicio, presence: true
  validate :valida_valores_variaveis_criacao

  def initialize(params = {})
    @nome = params[:nome]
    @data_inicio = params[:data_inicio]
    @descricao = params[:descricao]
    @valores_variaveis = params[:valores_variaveis]
  end
end
