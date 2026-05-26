# frozen_string_literal: true

class VariaveisController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def index
    variaveis = @estudo.variaveis

    if params[:nivel_aplicacao].present?
      unless Variavel.nivel_aplicacoes.key?(params[:nivel_aplicacao])
        render json: { erro: "Nível de aplicação inválido" }, status: :unprocessable_entity
        return
      end

      variaveis = variaveis.por_nivel(params[:nivel_aplicacao])
    end

    render json: variaveis, status: :ok
  end

  private

  def autorizar_acesso_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end
end
