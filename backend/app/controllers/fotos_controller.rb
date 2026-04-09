# frozen_string_literal: true

class FotosController < ApplicationController
  def show
    caminho = Rails.root.join("storage", "fotos", "estudos", params[:estudo_id], params[:tipo], params[:arquivo])

    if File.exist?(caminho)
      send_file caminho, disposition: :inline
    else
      head :not_found
    end
  end
end
