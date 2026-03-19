# frozen_string_literal: true

class CriarEspecieDto
  include ActiveModel::API

  CAMPOS = %i[foto classe genero nome_popular nome_cientifico status_conservacao nativa_da_regiao].freeze

  attr_accessor(*CAMPOS)

  validates :nome_cientifico, presence: true

  def initialize(params = {})
    CAMPOS.each do |campo|
      instance_variable_set(:"@#{campo}", params[campo])
    end
  end

  def atributos
    CAMPOS.each_with_object({}) do |campo, hash|
      hash[campo] = send(campo)
    end
  end
end
