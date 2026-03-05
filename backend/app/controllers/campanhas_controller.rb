# frozen_string_literal: true

class CampanhasController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :definir_campanha, only: %i[show update destroy]

  def index
    campanhas = servico.listar(estudo_id: @estudo.id)
    render json: campanhas, status: :ok
  end

  def show
    render json: @campanha, status: :ok
  end

  def create
    dto = CadastrarCampanhaDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    campanha = servico.cadastrar(
      estudo: @estudo,
      nome: dto.nome,
      data_inicio: dto.data_inicio,
      data_fim: dto.data_fim,
      descricao: dto.descricao,
      valores_variaveis: dto.valores_variaveis,
    )

    if campanha.persisted?
      render json: campanha, status: :created
    else
      render json: { erros: campanha.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    dto = EditarCampanhaDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    campanha = servico.atualizar(
      campanha: @campanha,
      nome: dto.nome,
      data_inicio: dto.data_inicio,
      data_fim: dto.data_fim,
      descricao: dto.descricao,
      valores_variaveis: dto.valores_variaveis,
    )

    if campanha.errors.empty?
      render json: campanha, status: :ok
    else
      render json: { erros: campanha.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    servico.excluir(campanha: @campanha)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoCampanha.new
  end

  def definir_estudo
    @estudo = Estudo.find(params[:estudo_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def definir_campanha
    @campanha = @estudo.campanhas.find(params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Campanha não encontrada" }, status: :not_found
  end
end
