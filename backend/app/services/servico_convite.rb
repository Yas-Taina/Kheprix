# frozen_string_literal: true

class ServicoConvite
  def criar(estudo:, proprietario:, email_convidado:)
    convite_existente = Convite.find_by(estudo_id: estudo.id, email_convidado: email_convidado, status: :pendente)
    if convite_existente
      return { erro: "Este email já possui um convite pendente para este estudo", status: :unprocessable_entity }
    end

    convite = Convite.new(
      estudo: estudo,
      proprietario_envio: proprietario,
      email_convidado: email_convidado,
    )

    if convite.save
      enviar_email(convite)
      convite
    else
      convite
    end
  end

  def listar(estudo_id:, status: nil)
    Convite.where(estudo_id: estudo_id).por_status(status).order(created_at: :desc)
  end

  def cancelar(convite:)
    unless convite.pendente?
      return { erro: "Apenas convites pendentes podem ser cancelados", status: :unprocessable_entity }
    end

    convite.destroy
    nil
  end

  private

  def enviar_email(convite)
    usuario_existente = Usuario.find_by(email: convite.email_convidado)

    if usuario_existente
      ConviteMailer.convite(convite: convite).deliver_later
    else
      ConviteMailer.convite_usuario_nao_cadastrado(convite: convite).deliver_later
    end
  end
end
