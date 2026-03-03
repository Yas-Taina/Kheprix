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

  def solicitar_redefinicao
    dto = SolicitarRedefinicaoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    ServicoAutenticacao.new.solicitar_redefinicao_senha(email: dto.email)

    render json: { mensagem: "Se o email existir, enviaremos instruções para redefinição de senha" }, status: :ok
  end

  def validar_token_redefinicao
    dto = ValidarTokenRedefinicaoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    usuario = ServicoAutenticacao.new.validar_token_redefinicao(token: dto.token)

    if usuario
      render json: { valido: true }, status: :ok
    else
      render json: { erro: "Token inválido ou expirado" }, status: :unauthorized
    end
  end

  def redefinir_senha
    dto = RedefinirSenhaDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    usuario = ServicoAutenticacao.new.redefinir_senha(token: dto.token, nova_senha: dto.nova_senha)

    if usuario
      render json: { mensagem: "Senha redefinida com sucesso" }, status: :ok
    else
      render json: { erro: "Token inválido ou expirado" }, status: :unauthorized
    end
  end
end
