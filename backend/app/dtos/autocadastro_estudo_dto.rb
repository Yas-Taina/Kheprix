# frozen_string_literal: true

class AutocadastroEstudoDto
  include ActiveModel::API

  attr_accessor :codigo, :senha_autocadastro

  validates :codigo, presence: true
  validates :senha_autocadastro, presence: true

  def initialize(params = {})
    @codigo = params[:codigo]
    @senha_autocadastro = params[:senha_autocadastro]
  end
end
