# frozen_string_literal: true

module EstudoAcessavel
  extend ActiveSupport::Concern

  included do
    after_action :registrar_ultimo_acesso!, if: :deve_registrar_ultimo_acesso?
  end

  private

  def definir_estudo
    @estudo = Estudo.find(params[:estudo_id] || params[:id])
  rescue ActiveRecord::RecordNotFound
    render json: { erro: "Estudo não encontrado" }, status: :not_found
  end

  def autorizar_acesso_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador
      render json: { erro: "Acesso negado a este estudo" }, status: :forbidden
    end
  end

  def autorizar_proprietario_estudo!
    colaborador = Colaborador.find_by(estudo_id: @estudo.id, usuario_id: usuario_atual.id)
    unless colaborador&.proprietario?
      render json: { erro: "Apenas proprietários podem realizar esta ação" }, status: :forbidden
    end
  end

  def registrar_ultimo_acesso!
    return if usuario_atual.ultimo_estudo_acessado_id == @estudo.id

    usuario_atual.update_column(:ultimo_estudo_acessado_id, @estudo.id)
  end

  def deve_registrar_ultimo_acesso?
    usuario_atual.present? && @estudo.present? && response.successful?
  end
end
