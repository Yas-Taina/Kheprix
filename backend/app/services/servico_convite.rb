# frozen_string_literal: true

class ServicoConvite
  def criar(estudo:, proprietario:, email_convidado:)
    convite_existente = Convite.find_by(estudo_id: estudo.id, email_convidado: email_convidado, status: :pendente)
    if convite_existente
      return { erro: "Este email já possui um convite pendente para este estudo", status: :unprocessable_entity }
    end

    usuario_existente = Usuario.find_by(email: email_convidado)
    if usuario_existente && Colaborador.exists?(estudo_id: estudo.id, usuario_id: usuario_existente.id)
      return { erro: "Este usuário já é colaborador deste estudo", status: :unprocessable_entity }
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

  def listar_recebidos(email:)
    Convite.where(email_convidado: email)
           .includes(:estudo, :proprietario_envio)
           .order(created_at: :desc)
  end

  def buscar_por_token(token:)
    Convite.includes(:estudo, :proprietario_envio).find_by(token: token)
  end

  def aceitar(convite:, usuario:)
    resultado = validar_processamento(convite)
    return resultado if resultado

    ActiveRecord::Base.transaction do
      convite.aceito!
      Colaborador.create!(
        estudo_id: convite.estudo_id,
        usuario_id: usuario.id,
        perfil: :colaborador,
      )
    end

    { mensagem: "Convite aceito com sucesso" }
  end

  def recusar(convite:)
    resultado = validar_processamento(convite)
    return resultado if resultado

    convite.recusado!
    { mensagem: "Convite recusado" }
  end

  private

  def validar_processamento(convite)
    if convite.data_expiracao.present? && convite.data_expiracao < Time.zone.now
      convite.expirado! if convite.pendente?
      return { erro: "Convite expirado", status: :unprocessable_entity }
    end

    unless convite.pendente?
      return { erro: "Convite já foi processado", status: :unprocessable_entity }
    end

    nil
  end

  def enviar_email(convite)
    usuario_existente = Usuario.find_by(email: convite.email_convidado)

    if usuario_existente
      ConviteMailer.convite(convite: convite).deliver_later
    else
      ConviteMailer.convite_usuario_nao_cadastrado(convite: convite).deliver_later
    end
  end
end
