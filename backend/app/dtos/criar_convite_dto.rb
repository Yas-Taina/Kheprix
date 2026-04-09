# frozen_string_literal: true

class CriarConviteDto
  include ActiveModel::API

  attr_accessor :email_convidado

  validates :email_convidado, presence: true, format: { with: URI::MailTo::EMAIL_REGEXP }

  def initialize(params = {})
    @email_convidado = params[:email_convidado]
  end
end
