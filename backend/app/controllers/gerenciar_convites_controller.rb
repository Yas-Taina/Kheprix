# frozen_string_literal: true

class GerenciarConvitesController < ApplicationController
  before_action :autenticar_requisicao!, except: %i[show]
  before_action :definir_convite, only: %i[show aceitar recusar]

  def index
    convites = servico.listar_recebidos(email: usuario_atual.email)
    render json: convites.map { |c| c.as_json(campos: :recebido) }, status: :ok
  end

  def show
    render json: @convite.as_json(campos: :publico), status: :ok
  end

  def aceitar
    resultado = servico.aceitar(convite: @convite, usuario: usuario_atual)

    if resultado[:erro]
      render json: { erro: resultado[:erro] }, status: resultado[:status]
      return
    end

    render json: resultado, status: :ok
  end

  def recusar
    resultado = servico.recusar(convite: @convite)

    if resultado[:erro]
      render json: { erro: resultado[:erro] }, status: resultado[:status]
      return
    end

    render json: resultado, status: :ok
  end

  private

  def servico
    @servico ||= ServicoConvite.new
  end

  def definir_convite
    @convite = servico.buscar_por_token(token: params[:token])
    unless @convite
      render json: { erro: "Convite não encontrado" }, status: :not_found
    end
  end
end
