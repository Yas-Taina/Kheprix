# frozen_string_literal: true

class ExportarDadosController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def exportar_dados
    dto = ExportarDadosDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    csv = ServicoExportacaoDados.new.gerar_csv(estudo: @estudo, agrupamento: dto.agrupamento)

    send_data "\xEF\xBB\xBF" + csv,
      type: "text/csv; charset=utf-8",
      disposition: "attachment",
      filename: "dados_estudo.csv"
  end

  private

  def definir_estudo
    @estudo = Estudo.find(params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def autorizar_acesso_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end
end
