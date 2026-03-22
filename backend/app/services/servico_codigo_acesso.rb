# frozen_string_literal: true

class ServicoCodigoAcesso
  def alterar_senha(estudo:, senha:)
    estudo.update!(senha_autocadastro: senha)
    estudo
  end

  def remover(estudo:)
    estudo.update!(codigo: nil, senha_autocadastro: nil)
  end

  def ingressar(codigo:, senha:, usuario:)
    estudo = Estudo.find_by(codigo: codigo)

    unless estudo
      return { erro: "Código inválido", status: :unprocessable_entity }
    end

    unless estudo.senha_autocadastro == senha
      return { erro: "Senha incorreta", status: :unprocessable_entity }
    end

    if Colaborador.exists?(estudo_id: estudo.id, usuario_id: usuario.id)
      return { erro: "Você já é colaborador deste estudo", status: :unprocessable_entity }
    end

    Colaborador.create!(estudo: estudo, usuario: usuario, perfil: :colaborador)
    { estudo_id: estudo.id, nome_estudo: estudo.nome, perfil: "colaborador" }
  end
end
