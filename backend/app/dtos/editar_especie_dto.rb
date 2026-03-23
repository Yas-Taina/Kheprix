# frozen_string_literal: true

class EditarEspecieDto
  include ActiveModel::API

  CAMPOS = %i[foto classe ordem familia genero especie nome_popular status_conservacao endemismo].freeze

  attr_accessor(*CAMPOS)

  def initialize(params = {})
    @chaves_informadas = []
    CAMPOS.each do |campo|
      if params.key?(campo)
        @chaves_informadas << campo
        instance_variable_set(:"@#{campo}", params[campo])
      end
    end
  end

  def atributos
    @chaves_informadas.each_with_object({}) do |campo, hash|
      hash[campo] = send(campo)
    end
  end
end
