# frozen_string_literal: true

class AtualizarRegistroOcorrenciaDto
  include ActiveModel::API

  attr_accessor :especie_id, :data, :hora, :latitude, :longitude, :qtde_individuos, :foto, :ausencia_especie

  validates :especie_id, presence: true
  validates :data, presence: true
  validates :hora, presence: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }

  def initialize(params = {})
    @chaves_informadas = []
    @especie_id = params[:especie_id]
    @data = params[:data]
    @hora = params[:hora]
    @latitude = params[:latitude]
    @longitude = params[:longitude]
    @qtde_individuos = params[:qtde_individuos]
    @foto = params[:foto]
    @ausencia_especie = params[:ausencia_especie]
    @chaves_informadas << :foto if params.key?(:foto)
  end

  def atributos
    hash = {
      especie_id:,
      data:,
      hora:,
      latitude:,
      longitude:,
      qtde_individuos:,
      ausencia_especie:
    }.compact
    hash[:foto] = foto if @chaves_informadas.include?(:foto)
    hash
  end
end
