# frozen_string_literal: true

class ServicoUsuario
  USUARIOS = [
    {
      id: 1,
      nome: "Mateus",
      email: "mateus@exemplo.com",
      senha_hash: BCrypt::Password.create("senha123")
    }
  ].freeze

  def buscar_por_email(email)
    USUARIOS.find { |u| u[:email] == email }
  end

  def buscar_por_id(id)
    USUARIOS.find { |u| u[:id] == id }
  end
end
