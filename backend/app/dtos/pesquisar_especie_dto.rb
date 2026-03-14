# frozen_string_literal: true

class PesquisarEspecieDto
  include ActiveModel::API

  attr_accessor :nome_popular, :nome_cientifico

  def initialize(params = {})
    @nome_popular = params[:nome_popular]
    @nome_cientifico = params[:nome_cientifico]
  end
end
