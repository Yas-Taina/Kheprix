# frozen_string_literal: true

class EspeciesController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!
  before_action :autorizar_proprietario_estudo!, only: %i[create update destroy]
  before_action :carregar_especie, only: %i[show update destroy]

  # GET /estudos/:estudo_id/especies
  def index
    dto = PesquisarEspecieDto.new(params)
    especies = servico.listar(estudo_id: @estudo.id, filtros: dto)
    render json: especies, status: :ok
  end

  # GET /estudos/:estudo_id/especies/:id
  def show
    render json: @especie, status: :ok
  end

  # POST /estudos/:estudo_id/especies
  def create
    dto = CriarEspecieDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    especie = servico.criar(estudo_id: @estudo.id, atributos: dto.atributos)

    if especie.persisted?
      render json: especie, status: :created
    else
      render json: { erros: especie.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # PATCH /estudos/:estudo_id/especies/:id
  def update
    dto = AtualizarEspecieDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    especie = servico.atualizar(especie: @especie, atributos: dto.atributos)

    if especie.errors.empty?
      render json: especie, status: :ok
    else
      render json: { erros: especie.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # DELETE /estudos/:estudo_id/especies/:id
  def destroy
    servico.destruir(@especie)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoEspecie.new
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

  def autorizar_proprietario_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador&.proprietario?
      render json: { erro: "Apenas proprietários podem realizar esta ação" }, status: :forbidden
    end
  end

  def carregar_especie
    @especie = servico.buscar_por_id(estudo_id: @estudo.id, id: params[:id])

    unless @especie
      render json: { erro: "Espécie não encontrada" }, status: :not_found
    end
  end
end
