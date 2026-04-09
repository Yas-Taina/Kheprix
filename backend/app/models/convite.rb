# frozen_string_literal: true

class Convite < ApplicationRecord
  belongs_to :estudo
  belongs_to :proprietario_envio, class_name: "Usuario"

  enum :status, { pendente: 0, aceito: 1, recusado: 2, expirado: 3 }

  validates :email_convidado, presence: true
  validates :token, presence: true, uniqueness: true

  before_validation :gerar_token, on: :create
  before_validation :definir_expiracao, on: :create

  scope :por_status, ->(status) { where(status: status) if status.present? }

  def as_json(options = {})
    campos = options.delete(:campos) || :completo

    case campos
    when :lista
      super(only: %i[id email_convidado status data_expiracao created_at], **options)
    when :recebido
      {
        id: id,
        estudo_id: estudo_id,
        nome_estudo: estudo.nome,
        nome_remetente: proprietario_envio.nome,
        status: status,
        data_expiracao: data_expiracao,
        created_at: created_at
      }
    when :publico
      {
        id: id,
        estudo_id: estudo_id,
        nome_estudo: estudo.nome,
        email_convidado: email_convidado,
        status: status,
        data_expiracao: data_expiracao
      }
    else
      super(only: %i[id estudo_id email_convidado token status data_expiracao created_at], **options)
    end
  end

  private

  def gerar_token
    self.token ||= SecureRandom.urlsafe_base64(32)
  end

  def definir_expiracao
    self.data_expiracao ||= 7.days.from_now
  end
end
