# frozen_string_literal: true

module AutenticavelJwt
  extend ActiveSupport::Concern

  private

  def autenticar_requisicao!
    token = extrair_token
    unless token
      render json: { erro: "Token não fornecido" }, status: :unauthorized
      return
    end

    usuario = ServicoAutenticacao.new.verificar_token(token: token)
    unless usuario
      render json: { erro: "Token inválido ou expirado" }, status: :unauthorized
      return
    end

    @usuario_atual = usuario
  end

  def usuario_atual
    @usuario_atual
  end

  def extrair_token
    header = request.headers["Authorization"]
    header&.split(" ")&.last
  end
end
