# frozen_string_literal: true

class EspeciesController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :carregar_especie, only: %i[show update destroy]

  # GET /especies
  def index
    especies = servico.listar
    render json: especies, status: :ok
  end

  # GET /especies/:id
  def show
    render json: @especie, status: :ok
  end

  # POST /especies
  def create
    dto = CriarEspecieDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    especie = servico.criar(
      foto: dto.foto,
      classe: dto.classe,
      genero: dto.genero,
      nome_popular: dto.nome_popular,
      nome_cientifico: dto.nome_cientifico,
      status_conservacao: dto.status_conservacao,
      nativa_da_regiao: dto.nativa_da_regiao,
    )

    if especie.persisted?
      render json: especie, status: :created
    else
      render json: { erros: especie.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # PATCH /especies/:id
  def update
    dto = AtualizarEspecieDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    especie = servico.atualizar(
      especie: @especie,
      foto: dto.foto,
      classe: dto.classe,
      genero: dto.genero,
      nome_popular: dto.nome_popular,
      nome_cientifico: dto.nome_cientifico,
      status_conservacao: dto.status_conservacao,
      nativa_da_regiao: dto.nativa_da_regiao,
    )

    if especie.errors.empty?
      render json: especie, status: :ok
    else
      render json: { erros: especie.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # DELETE /especies/:id
  def destroy
    servico.destruir(@especie)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoEspecie.new
  end

  def carregar_especie
    @especie = servico.buscar_por_id(params[:id])

    unless @especie
      render json: { erro: "Espécie não encontrada" }, status: :not_found
    end
  end
end