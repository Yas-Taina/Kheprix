# frozen_string_literal: true

module EstudoAcessavel
  extend ActiveSupport::Concern

  private

  def definir_estudo
    @estudo = Estudo.find(params[:estudo_id] || params[:id])
    registrar_ultimo_acesso!(@estudo)
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def registrar_ultimo_acesso!(estudo)
    return unless usuario_atual
    return if usuario_atual.ultimo_estudo_acessado_id == estudo.id

    usuario_atual.update_column(:ultimo_estudo_acessado_id, estudo.id)
  end
end
