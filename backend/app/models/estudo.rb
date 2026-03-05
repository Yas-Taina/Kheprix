# frozen_string_literal: true

class Estudo < ApplicationRecord
  has_many :colaboradores, dependent: :destroy
  has_many :usuarios, through: :colaboradores
  has_many :variaveis, dependent: :destroy
  has_many :campanhas, dependent: :destroy

  validates :nome, presence: true

  def as_json(options = {})
    super(only: %i[id nome observacoes created_at], **options)
  end
end
