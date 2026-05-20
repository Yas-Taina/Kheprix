# frozen_string_literal: true

class CriarEventoAmostragemDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :horario_inicio, :esforco_real, :valores_variaveis

  def self.human_attribute_name(attr, options = {})
    {
      "horario_inicio" => "Horário de início",
      "esforco_real" => "Esforço real"
    }[attr.to_s] || super
  end

  validates :horario_inicio, :esforco_real, presence: true
  validates :esforco_real, length: { maximum: 50 }, allow_blank: true
  validate :valida_valores_variaveis_criacao

  def initialize(params = {})
    @horario_inicio = params[:horario_inicio]
    @esforco_real = params[:esforco_real]
    @valores_variaveis = params[:valores_variaveis]
  end
end
