# frozen_string_literal: true

class AnalisesController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def executar
    dto = ExecutarAnaliseDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    resultado = ServicoAnalise.new.executar(
      estudo_id: @estudo.id,
      chave: dto.chave,
      params: dto.to_params,
    )

    if resultado[:erro]
      render json: { erro: resultado[:erro] }, status: :unprocessable_entity
    else
      render json: resposta_com_url_absoluta(resultado), status: :ok
    end
  end

  private

  def resposta_com_url_absoluta(resultado)
    path = resultado[:url_arquivo]
    url_absoluto = path.present? ? "#{ENV.fetch('BACKEND_URL', 'http://localhost:3000')}#{path}" : nil

    resposta = {
      analise: resultado[:analise],
      nome: resultado[:nome],
      valor: resultado[:valor],
      grafico: resultado[:grafico],
      urlArquivo: url_absoluto
    }
    resposta[:aviso] = resultado[:aviso] if resultado[:aviso].present?
    resposta
  end
end
