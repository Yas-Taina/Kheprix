# frozen_string_literal: true

class RegistroOcorrenciasController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!
  before_action :definir_campanha
  before_action :definir_unidade_amostral
  before_action :definir_evento_amostragem
  before_action :carregar_registro, only: %i[show update destroy]

  def index
    registros = servico.listar(evento_id: @evento_amostragem.id)
    render json: registros, status: :ok
  end

  def show
    render json: @registro, status: :ok
  end

  def create
    dto = CriarRegistroOcorrenciaDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    registro = servico.criar(evento_id: @evento_amostragem.id, atributos: dto.atributos)

    if registro.persisted?
      render json: registro, status: :created
    else
      render json: { erros: registro.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    dto = AtualizarRegistroOcorrenciaDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    registro = servico.atualizar(registro: @registro, atributos: dto.atributos)

    if registro.errors.empty?
      render json: registro, status: :ok
    else
      render json: { erros: registro.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    servico.destruir(@registro)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoRegistroOcorrencia.new
  end

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

  def definir_evento_amostragem
    @evento_amostragem = @unidade_amostral.eventos_amostragem.find(params[:evento_amostragem_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Evento de amostragem não encontrado" }, status: :not_found
  end

  def carregar_registro
    @registro = servico.buscar_por_id(evento_id: @evento_amostragem.id, id: params[:id])

    unless @registro
      render json: { erro: "Registro de ocorrência não encontrado" }, status: :not_found
    end
  end
end
