# frozen_string_literal: true

class DashboardController < ApplicationController
  before_action :autenticar_requisicao!

  def index
    resultado = servico.resumo_ultimo_estudo(usuario: usuario_atual)

    if resultado.nil?
      render json: { erro: "Nenhum estudo encontrado" }, status: :not_found
    else
      render json: resultado, status: :ok
    end
  end

  private

  def servico
    @servico ||= ServicoDashboard.new
  end
end
