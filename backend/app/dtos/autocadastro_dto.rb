# frozen_string_literal: true

require "uri"

class AutocadastroDto
  include ActiveModel::API

  attr_accessor :nome, :email, :senha

  validates :nome, presence: true
  validates :email, presence: true, format: { with: URI::MailTo::EMAIL_REGEXP, message: "deve ter formato válido (ex.: nome@dominio.com)" }
  validates :senha, presence: true, length: { minimum: 8, message: "deve ter pelo menos 8 caracteres" }

  def initialize(params = {})
    @nome = params[:nome]
    @email = params[:email]
    @senha = params[:senha]
  end
end
