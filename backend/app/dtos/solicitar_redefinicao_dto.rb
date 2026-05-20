# frozen_string_literal: true

class SolicitarRedefinicaoDto
  include ActiveModel::API

  attr_accessor :email

  validates :email, presence: true, format: { with: URI::MailTo::EMAIL_REGEXP, message: "tem formato inválido" }

  def initialize(params = {})
    @email = params[:email]
  end
end
