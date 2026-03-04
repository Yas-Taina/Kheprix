# frozen_string_literal: true

class CriarProjetoDto
  include ActiveModel::API

  attr_accessor :nome, :descricao

  validates :nome, presence: true

  def initialize(params = {})
    @nome = params[:nome]
    @descricao = params[:descricao]
  end
end