# frozen_string_literal: true

class SolicitarRedefinicaoDto
  include ActiveModel::API

  attr_accessor :email

  validates :email, presence: true

  def initialize(params = {})
    @email = params[:email]
  end
end
