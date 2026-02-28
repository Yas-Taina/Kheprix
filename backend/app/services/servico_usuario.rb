# frozen_string_literal: true

class ServicoUsuario
  def buscar_por_email(email)
    Usuario.find_by(email: email&.downcase&.strip)
  end

  def buscar_por_id(id)
    Usuario.find_by(id: id)
  end

  def cadastrar(nome:, email:, senha:)
    usuario = Usuario.new(nome: nome, email: email, password: senha)
    usuario.save
    usuario
  end
end
