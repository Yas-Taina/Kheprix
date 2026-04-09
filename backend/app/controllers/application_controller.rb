# frozen_string_literal: true

class ApplicationController < ActionController::API
  include AutenticavelJwt
  wrap_parameters false

  rescue_from FotoExcedeTamanhoMaximo, with: :foto_excede_tamanho_maximo

  private

  def foto_excede_tamanho_maximo(exception)
    render json: { erro: exception.message }, status: :unprocessable_entity
  end
end
