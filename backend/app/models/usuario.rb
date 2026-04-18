# frozen_string_literal: true

class Usuario < ApplicationRecord
  has_secure_password

  belongs_to :ultimo_estudo_acessado, class_name: "Estudo", optional: true

  validates :nome, presence: true
  validates :email, presence: true, uniqueness: { case_sensitive: false }, format: { with: URI::MailTo::EMAIL_REGEXP }

  before_validation :normalizar_email

  def as_json(options = {})
    super(only: %i[id nome email created_at], **options)
  end

  private

  def normalizar_email
    self.email = email&.downcase&.strip
  end
end
