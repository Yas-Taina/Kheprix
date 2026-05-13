# frozen_string_literal: true

class RedefinirSenhaDto
  include ActiveModel::API

  attr_accessor :token, :nova_senha

  validates :token, presence: true
  validates :nova_senha, presence: true, length: { minimum: 8, message: "deve ter pelo menos 8 caracteres" }

  def initialize(params = {})
    @token = params[:token]
    @nova_senha = params[:nova_senha]
  end
end
