# frozen_string_literal: true

class UsuarioMailer < ApplicationMailer
  def redefinicao_senha(usuario:, token:)
    @usuario = usuario
    @token = token
    @link = "#{ENV.fetch('FRONTEND_URL')}" \
            "#{ENV.fetch('REDEFINICAO_SENHA_CAMINHO')}?token=#{token}"

    mail(to: @usuario.email, subject: "Redefinição de senha")
  end
end
