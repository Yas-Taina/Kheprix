# frozen_string_literal: true

class CriarEventoAmostragemDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :horario_inicio, :horario_fim, :esforco_real, :valores_variaveis

  validates :horario_inicio, :horario_fim, :esforco_real, presence: true
  validate :valida_valores_variaveis_criacao

  def initialize(params = {})
    @horario_inicio = params[:horario_inicio]
    @horario_fim = params[:horario_fim]
    @esforco_real = params[:esforco_real]
    @valores_variaveis = params[:valores_variaveis]
  end
end
