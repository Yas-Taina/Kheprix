# frozen_string_literal: true

class EstudosController < ApplicationController
  before_action :autenticar_requisicao!

  def index
    render json: { erro: "Não implementado" }, status: :not_implemented
  end

  def show
    render json: { erro: "Não implementado" }, status: :not_implemented
  end

  def create
    dto = CadastrarEstudoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    estudo = servico.cadastrar(nome: dto.nome, observacoes: dto.observacoes, usuario: usuario_atual, variaveis: dto.variaveis)

    if estudo.persisted?
      render json: estudo, status: :created
    else
      render json: { erros: estudo.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    render json: { erro: "Não implementado" }, status: :not_implemented
  end

  def destroy
    render json: { erro: "Não implementado" }, status: :not_implemented
  end

  private

  def servico
    @servico ||= ServicoEstudo.new
  end
end
