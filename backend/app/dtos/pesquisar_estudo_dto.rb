# frozen_string_literal: true

class PesquisarEstudoDto
  include ActiveModel::API

  attr_accessor :nome, :criado_a_partir_de, :criado_ate, :atualizado_a_partir_de, :atualizado_ate

  validate :validar_formato_datas

  def initialize(params = {})
    @nome = params[:nome]
    @criado_a_partir_de = params[:criado_a_partir_de]
    @criado_ate = params[:criado_ate]
    @atualizado_a_partir_de = params[:atualizado_a_partir_de]
    @atualizado_ate = params[:atualizado_ate]
  end

  private

  def validar_formato_datas
    %i[criado_a_partir_de criado_ate atualizado_a_partir_de atualizado_ate].each do |campo|
      valor = send(campo)
      next if valor.blank?

      Date.parse(valor.to_s)
    rescue Date::Error
      errors.add(campo, "deve ser uma data válida (YYYY-MM-DD)")
    end
  end
end
