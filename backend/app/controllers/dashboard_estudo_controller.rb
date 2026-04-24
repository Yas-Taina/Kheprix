# frozen_string_literal: true

class DashboardEstudoController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def show
    resultado = servico.dados_completos(estudo: @estudo)
    render json: resultado, status: :ok
  end

  private

  def servico
    @servico ||= ServicoDashboardEstudo.new
  end
end
