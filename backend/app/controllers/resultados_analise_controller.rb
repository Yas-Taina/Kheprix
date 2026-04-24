# frozen_string_literal: true

class ResultadosAnaliseController < ApplicationController
  include EstudoAcessavel

  before_action :autenticar_requisicao!
  before_action :definir_estudo
  before_action :autorizar_acesso_estudo!

  def show
    caminho = Rails.root.join(
      "storage", "analises", "estudos",
      params[:estudo_id], params[:chave], params[:arquivo],
    )

    if File.exist?(caminho)
      send_file caminho, type: "application/zip", disposition: "attachment"
    else
      head :not_found
    end
  end
end
