# frozen_string_literal: true

class CadastrarUnidadeAmostralDto
  include ActiveModel::API
  include ValidaValoresVariaveis

  attr_accessor :nome, :latitude, :longitude, :raio, :metodo_coleta, :esforco_amostral, :valores_variaveis

  def self.human_attribute_name(attr, options = {})
    {
      "metodo_coleta" => "Método de coleta",
      "esforco_amostral" => "Esforço amostral"
    }[attr.to_s] || super
  end

  validates :nome, presence: true, length: { maximum: 120 }
  validates :metodo_coleta, length: { maximum: 50 }, allow_blank: true
  validates :esforco_amostral, length: { maximum: 50 }, allow_blank: true
  validates :latitude, presence: true, numericality: { greater_than_or_equal_to: -90, less_than_or_equal_to: 90 }
  validates :longitude, presence: true, numericality: { greater_than_or_equal_to: -180, less_than_or_equal_to: 180 }
  validate :valida_valores_variaveis_criacao

  def initialize(params = {})
    @nome = params[:nome]
    @latitude = params[:latitude]
    @longitude = params[:longitude]
    @raio = params[:raio]
    @metodo_coleta = params[:metodo_coleta]
    @esforco_amostral = params[:esforco_amostral]
    @valores_variaveis = params[:valores_variaveis]
  end
end
