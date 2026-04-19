# frozen_string_literal: true

class UnidadesAmostraisController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!
  before_action :definir_campanha
  before_action :autorizar_proprietario_estudo!, only: %i[destroy]
  before_action :definir_unidade, only: %i[show update destroy]

  def index
    unidades = servico.listar(campanha_id: @campanha.id)
    render json: unidades, status: :ok
  end

  def show
    render json: @unidade, status: :ok
  end

  def create
    dto = CadastrarUnidadeAmostralDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    unidade = servico.cadastrar(
      campanha: @campanha,
      nome: dto.nome,
      latitude: dto.latitude,
      longitude: dto.longitude,
      raio: dto.raio,
      metodo_coleta: dto.metodo_coleta,
      esforco_amostral: dto.esforco_amostral,
      valores_variaveis: dto.valores_variaveis,
    )

    if unidade.persisted?
      render json: unidade, status: :created
    else
      render json: { erros: unidade.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    dto = EditarUnidadeAmostralDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    unidade = servico.atualizar(
      unidade: @unidade,
      nome: dto.nome,
      latitude: dto.latitude,
      longitude: dto.longitude,
      raio: dto.raio,
      metodo_coleta: dto.metodo_coleta,
      esforco_amostral: dto.esforco_amostral,
      valores_variaveis: dto.valores_variaveis,
    )

    if unidade.errors.empty?
      render json: unidade, status: :ok
    else
      render json: { erros: unidade.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    servico.excluir(unidade: @unidade)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoUnidadeAmostral.new
  end

  def definir_campanha
    @campanha = @estudo.campanhas.find(params[:campanha_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Campanha não encontrada" }, status: :not_found
  end

  def definir_unidade
    @unidade = @campanha.unidades_amostrais.find(params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Unidade amostral não encontrada" }, status: :not_found
  end
end
