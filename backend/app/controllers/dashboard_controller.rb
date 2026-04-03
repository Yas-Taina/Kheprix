# frozen_string_literal: true

class DashboardController < ApplicationController
  before_action :autenticar_requisicao!

  def index
    resultado = servico.resumo_ultimo_estudo(usuario: usuario_atual)

    if resultado.nil?
      render json: { mensagem: "Nenhum estudo encontrado" }, status: :ok
    else
      render json: resultado, status: :ok
    end
  end

  private

  def servico
    @servico ||= ServicoDashboard.new
  end
end
