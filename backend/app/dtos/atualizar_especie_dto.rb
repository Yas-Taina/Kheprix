# frozen_string_literal: true

class AtualizarEspecieDto
  include ActiveModel::API

  attr_accessor :foto,
                :classe,
                :genero,
                :nome_popular,
                :nome_cientifico,
                :status_conservacao,
                :nativa_da_regiao

  def initialize(params = {})
    @foto = params[:foto]
    @classe = params[:classe]
    @genero = params[:genero]
    @nome_popular = params[:nome_popular]
    @nome_cientifico = params[:nome_cientifico]
    @status_conservacao = params[:status_conservacao]
    @nativa_da_regiao = params[:nativa_da_regiao]
  end
end