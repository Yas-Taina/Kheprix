# frozen_string_literal: true

class CodigoAcessoDto
  include ActiveModel::API

  attr_accessor :senha_autocadastro

  validates :senha_autocadastro, presence: true

  def initialize(params = {})
    @senha_autocadastro = params[:senha_autocadastro]
  end
end
