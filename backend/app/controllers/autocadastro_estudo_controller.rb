# frozen_string_literal: true

class AutocadastroEstudoController < ApplicationController
  before_action :autenticar_requisicao!

  def create
    dto = AutocadastroEstudoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    resultado = servico.ingressar(codigo: dto.codigo, senha: dto.senha_autocadastro, usuario: usuario_atual)

    if resultado[:erro]
      render json: { erro: resultado[:erro] }, status: resultado[:status]
      return
    end

    render json: resultado, status: :created
  end

  private

  def servico
    @servico ||= ServicoCodigoAcesso.new
  end
end
