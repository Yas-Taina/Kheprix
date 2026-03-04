# frozen_string_literal: true

class Projeto < ApplicationRecord
  # 1. Associações
  has_many :tarefas, dependent: :destroy   # Exemplo: se Projeto tiver filhos

  # 2. Validações (regras de negócio)
  validates :nome, presence: true
  validates :nome, uniqueness: true        # Unicidade é regra de negócio → model

  # 3. Scopes
  scope :ativos, -> { where(ativo: true) }
  scope :recentes, -> { order(created_at: :desc) }

  # 4. Serialização — controla quais campos aparecem no JSON de resposta
  def as_json(options = {})
    super(only: %i[id nome descricao ativo created_at], **options)
  end
end