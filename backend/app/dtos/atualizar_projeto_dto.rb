# frozen_string_literal: true

class AtualizarProjetoDto
  include ActiveModel::API

  attr_accessor :nome, :descricao, :ativo

  validates :nome, presence: true

  def initialize(params = {})
    @nome = params[:nome]
    @descricao = params[:descricao]
    @ativo = params[:ativo]
  end
end