# frozen_string_literal: true

class CriarRegistroOcorrenciaDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :especie_id, :data, :hora, :latitude, :longitude, :qtde_individuos, :foto, :ausencia_especie,
                :valores_variaveis

  validates :especie_id, presence: true
  validates :data, presence: true
  validates :hora, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }
  validate :valida_valores_variaveis_criacao

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
end
