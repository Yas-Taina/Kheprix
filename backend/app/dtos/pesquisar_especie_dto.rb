# frozen_string_literal: true

class PesquisarEspecieDto
  include ActiveModel::API

  attr_accessor :nome_popular

  def initialize(params = {})
    @nome_popular = params[:nome_popular]
  end
end
