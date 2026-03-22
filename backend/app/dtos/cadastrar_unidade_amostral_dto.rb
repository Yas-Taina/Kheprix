# frozen_string_literal: true

class CadastrarUnidadeAmostralDto
  include ActiveModel::API

  attr_accessor :nome, :latitude, :longitude, :raio, :metodo_coleta, :esforco_amostral

  validates :latitude, presence: true
  validates :longitude, presence: true

  def initialize(params = {})
    @nome = params[:nome]
    @latitude = params[:latitude]
    @longitude = params[:longitude]
    @raio = params[:raio]
    @metodo_coleta = params[:metodo_coleta]
    @esforco_amostral = params[:esforco_amostral]
  end
end
