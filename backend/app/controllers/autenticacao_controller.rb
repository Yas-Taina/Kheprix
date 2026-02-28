# frozen_string_literal: true

class AutenticacaoController < ApplicationController
  def login
    dto = LoginDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    token = ServicoAutenticacao.new.autenticar(email: dto.email, senha: dto.senha)

    if token
      render json: { token: token }, status: :ok
    else
      render json: { erro: "Email ou senha inválidos" }, status: :unauthorized
    end
  end
end
