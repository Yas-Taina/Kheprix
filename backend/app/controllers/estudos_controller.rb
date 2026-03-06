# frozen_string_literal: true

class EstudosController < ApplicationController
  before_action :autenticar_requisicao!

  def index
    dto = PesquisarEstudoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    estudos = servico.pesquisar(usuario: usuario_atual, filtros: dto)
    render json: estudos, status: :ok
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
    resultado = servico.deletar(id: params[:id], usuario: usuario_atual)

    case resultado
    when :ok
      head :no_content
    when :descadastrado
      render json: { mensagem: "Você foi descadastrado do estudo" }, status: :ok
    when :nao_encontrado
      render json: { erro: "Estudo não encontrado" }, status: :not_found
    when :nao_autorizado
      render json: { erro: "Permissão negada" }, status: :forbidden
    end
  end

  private

  def servico
    @servico ||= ServicoEstudo.new
  end
end
