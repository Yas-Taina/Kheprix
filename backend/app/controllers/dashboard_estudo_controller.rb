# frozen_string_literal: true

class DashboardEstudoController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  # GET /estudos/:id/dashboard
  def show
    resultado = servico.dados_completos(estudo_id: @estudo.id)
    render json: resultado, status: :ok
  end

  private

  def servico
    @servico ||= ServicoDashboardEstudo.new
  end

  def definir_estudo
    @estudo = Estudo.find(params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def autorizar_acesso_estudo!
    unless Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end
end
