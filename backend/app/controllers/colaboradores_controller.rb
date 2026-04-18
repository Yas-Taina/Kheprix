# frozen_string_literal: true

class ColaboradoresController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!
  before_action :autorizar_proprietario_estudo!, only: %i[update destroy]
  before_action :definir_colaborador, only: %i[update destroy]

  def index
    colaboradores = servico.listar(estudo_id: @estudo.id)
    render json: colaboradores, status: :ok
  end

  def update
    dto = AlterarPerfilColaboradorDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    servico.alterar_perfil(colaborador: @colaborador, perfil: dto.perfil)
    render json: @colaborador.reload, status: :ok
  end

  def destroy
    if @colaborador.usuario_id == usuario_atual.id
      render json: { erro: "Não é possível remover a si mesmo" }, status: :unprocessable_entity
      return
    end

    servico.remover(colaborador: @colaborador)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoColaborador.new
  end

  def autorizar_acesso_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end

  def autorizar_proprietario_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador&.proprietario?
      render json: { erro: "Apenas proprietários podem realizar esta ação" }, status: :forbidden
    end
  end

  def definir_colaborador
    @colaborador = Colaborador.includes(:usuario).find_by(estudo_id: @estudo.id, usuario_id: params[:id])
    unless @colaborador
      render json: { erro: "Colaborador não encontrado" }, status: :not_found
    end
  end
end
