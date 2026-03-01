# frozen_string_literal: true

class AutocadastroDto
  include ActiveModel::API

  attr_accessor :nome, :email, :senha

  validates :nome, presence: true
  validates :email, presence: true
  validates :senha, presence: true

  def initialize(params = {})
    @nome = params[:nome]
    @email = params[:email]
    @senha = params[:senha]
  end
end
