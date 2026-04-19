# frozen_string_literal: true

class ExportarDadosController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def exportar_dados
    dto = ExportarDadosDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    servico = ServicoExportacaoDados.new

    if dto.formato == "xml"
      xml = servico.gerar_xml(estudo: @estudo)
      send_data xml,
        type: "application/xml; charset=utf-8",
        disposition: "attachment",
        filename: "dados_estudo.xml"
    elsif dto.formato == "csv"
      csv = servico.gerar_csv(estudo: @estudo, agrupamento: dto.agrupamento)
      send_data "\xEF\xBB\xBF" + csv,
        type: "text/csv; charset=utf-8",
        disposition: "attachment",
        filename: "dados_estudo.csv"
    else
      render json: { erro: "Formato não suportado" }, status: :unprocessable_entity
    end
  end
end
