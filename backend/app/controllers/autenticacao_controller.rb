# frozen_string_literal: true

class AutenticacaoController < ApplicationController
  before_action :autenticar_requisicao!, only: %i[me]

  def login
    parametros = params.expect(autenticacao: [ :email, :senha ])
    token = ServicoAutenticacao.new.autenticar(email: parametros[:email], senha: parametros[:senha])

    if token
      render json: { token: token }, status: :ok
    else
      render json: { erro: "Email ou senha inválidos" }, status: :unauthorized
    end
  end

  def me
    render json: { usuario: usuario_atual }, status: :ok
  end
end
