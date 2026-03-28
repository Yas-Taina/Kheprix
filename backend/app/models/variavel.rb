# frozen_string_literal: true

class Variavel < ApplicationRecord
  include SoftDeletavel

  self.table_name = "variaveis"

  belongs_to :estudo
  has_many :valores_variaveis, dependent: :destroy

  enum :nivel_aplicacao, { campanha: 0, unidade: 1, evento: 2, registro: 3 }
  enum :tipo_dado, { string: 0, number: 1, date: 2 }

  validates :nome, presence: true
  validates :nivel_aplicacao, presence: true
  validates :tipo_dado, presence: true

  scope :por_nivel, ->(nivel) { where(nivel_aplicacao: nivel) }

  def as_json(options = {})
    super(only: %i[id nome metrica nivel_aplicacao tipo_dado created_at updated_at], **options)
  end
end
