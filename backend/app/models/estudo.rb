# frozen_string_literal: true

class Estudo < ApplicationRecord
  has_many :colaboradores, class_name: "Colaborador", dependent: :destroy
  has_many :usuarios, through: :colaboradores
  has_many :variaveis, dependent: :destroy

  validates :nome, presence: true

  scope :por_usuario, ->(usuario) { joins(:colaboradores).where(colaboradores: { usuario_id: usuario.id }) }
  scope :por_nome, ->(nome) { where("nome ILIKE ?", "%#{nome}%") }
  scope :criado_a_partir_de, ->(data) { where("estudos.created_at >= ?", data) }
  scope :criado_ate, ->(data) { where("estudos.created_at <= ?", data.to_date.end_of_day) }
  scope :atualizado_a_partir_de, ->(data) { where("estudos.updated_at >= ?", data) }
  scope :atualizado_ate, ->(data) { where("estudos.updated_at <= ?", data.to_date.end_of_day) }

  def as_json(options = {})
    super(only: %i[id nome observacoes created_at updated_at], **options)
  end
end
