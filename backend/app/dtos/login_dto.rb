# frozen_string_literal: true

class LoginDto
  include ActiveModel::API

  attr_accessor :email, :senha

  validates :email, presence: true
  validates :senha, presence: true

  def initialize(params = {})
    @email = params[:email]
    @senha = params[:senha]
  end
end
