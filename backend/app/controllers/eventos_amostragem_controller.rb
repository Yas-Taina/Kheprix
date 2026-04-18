# frozen_string_literal: true

class EventosAmostragemController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!
  before_action :definir_campanha
  before_action :definir_unidade_amostral
  before_action :autorizar_proprietario_estudo!, only: %i[destroy]
  before_action :definir_evento, only: %i[show update destroy]

  def index
    eventos = servico.listar(unidade_amostral_id: @unidade_amostral.id)
    render json: eventos, status: :ok
  end

  def show
    render json: @evento, status: :ok
  end

  def create
    dto = CriarEventoAmostragemDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    evento = servico.criar(
      unidade_amostral: @unidade_amostral,
      horario_inicio: dto.horario_inicio,
      horario_fim: dto.horario_fim,
      esforco_real: dto.esforco_real,
    )

    if evento.persisted?
      render json: evento, status: :created
    else
      render json: { erros: evento.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    dto = AtualizarEventoAmostragemDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    evento = servico.atualizar(
      evento: @evento,
      horario_inicio: dto.horario_inicio,
      horario_fim: dto.horario_fim,
      esforco_real: dto.esforco_real,
    )

    if evento.errors.empty?
      render json: evento, status: :ok
    else
      render json: { erros: evento.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    servico.excluir(evento: @evento)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoEventoAmostragem.new
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

  def definir_campanha
    @campanha = @estudo.campanhas.find(params[:campanha_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Campanha não encontrada" }, status: :not_found
  end

  def definir_unidade_amostral
    @unidade_amostral = @campanha.unidades_amostrais.find(params[:unidade_amostral_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Unidade amostral não encontrada" }, status: :not_found
  end

  def definir_evento
    @evento = @unidade_amostral.eventos_amostragem.find(params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Evento de amostragem não encontrado" }, status: :not_found
  end
end
