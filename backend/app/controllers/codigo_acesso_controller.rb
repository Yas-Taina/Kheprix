# frozen_string_literal: true

class CodigoAcessoController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_proprietario_estudo!

  def show
    render json: resposta_codigo, status: :ok
  end

  def update
    dto = CodigoAcessoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    servico.alterar_senha(estudo: @estudo, senha: dto.senha_autocadastro)
    render json: resposta_codigo, status: :ok
  end

  private

  def servico
    @servico ||= ServicoCodigoAcesso.new
  end

  def resposta_codigo
    { codigo: @estudo.codigo, senha_autocadastro: @estudo.senha_autocadastro }
  end

  def definir_estudo
    @estudo = Estudo.find(params[:estudo_id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def autorizar_proprietario_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador&.proprietario?
      render json: { erro: "Apenas proprietários podem realizar esta ação" }, status: :forbidden
    end
  end
end
