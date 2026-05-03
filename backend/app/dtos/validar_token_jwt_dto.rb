# frozen_string_literal: true

class ValidarTokenJwtDto
  include ActiveModel::API

  attr_accessor :token

  validates :token, presence: true

  def initialize(params = {})
    @token = params[:token]
  end
end
