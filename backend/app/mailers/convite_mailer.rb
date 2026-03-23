# frozen_string_literal: true

class ConviteMailer < ApplicationMailer
  def convite(convite:)
    @convite = convite
    @nome_estudo = convite.estudo.nome
    @nome_remetente = convite.proprietario_envio.nome
    @link = "#{ENV.fetch('FRONTEND_URL')}#{ENV.fetch('CONVITE_CAMINHO', '/convites')}/#{convite.token}"

    mail(to: convite.email_convidado, subject: "Convite para o estudo: #{@nome_estudo}")
  end

  def convite_usuario_nao_cadastrado(convite:)
    @convite = convite
    @nome_estudo = convite.estudo.nome
    @nome_remetente = convite.proprietario_envio.nome
    @link_cadastro = "#{ENV.fetch('FRONTEND_URL')}#{ENV.fetch('CADASTRO_CAMINHO', '/cadastro')}"

    mail(to: convite.email_convidado, subject: "Convite para o estudo: #{@nome_estudo}")
  end
end
