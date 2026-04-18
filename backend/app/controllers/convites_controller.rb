# frozen_string_literal: true

class ConvitesController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_proprietario_estudo!
  before_action :definir_convite, only: %i[destroy]

  def index
    convites = servico.listar(estudo_id: @estudo.id, status: params[:status])
    render json: convites.map { |c| c.as_json(campos: :lista) }, status: :ok
  end

  def create
    dto = CriarConviteDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    resultado = servico.criar(
      estudo: @estudo,
      proprietario: usuario_atual,
      email_convidado: dto.email_convidado,
    )

    if resultado.is_a?(Hash) && resultado[:erro]
      render json: { erro: resultado[:erro] }, status: resultado[:status]
      return
    end

    if resultado.persisted?
      render json: resultado, status: :created
    else
      render json: { erros: resultado.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    resultado = servico.cancelar(convite: @convite)

    if resultado.is_a?(Hash) && resultado[:erro]
      render json: { erro: resultado[:erro] }, status: resultado[:status]
      return
    end

    head :no_content
  end

  private

  def servico
    @servico ||= ServicoConvite.new
  end

  def autorizar_proprietario_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador&.proprietario?
      render json: { erro: "Apenas proprietários podem realizar esta ação" }, status: :forbidden
    end
  end

  def definir_convite
    @convite = Convite.find_by(id: params[:id], estudo_id: @estudo.id)
    unless @convite
      render json: { erro: "Convite não encontrado" }, status: :not_found
    end
  end
end
