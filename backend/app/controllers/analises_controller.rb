# frozen_string_literal: true

class AnalisesController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def executar
    dto = ExecutarAnaliseDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    resultado = ServicoAnalise.new.executar(
      estudo_id: @estudo.id,
      chave: dto.chave,
      params: dto.to_params,
    )

    if resultado[:erro]
      render json: { erro: resultado[:erro] }, status: :unprocessable_entity
    else
      render json: resultado, status: :ok
    end
  end

  private

  def definir_estudo
    @estudo = Estudo.find(params[:estudo_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def autorizar_acesso_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end
end
