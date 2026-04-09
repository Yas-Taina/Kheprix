# frozen_string_literal: true

class AtualizarEspecieDto
  include ActiveModel::API

  CAMPOS = %i[foto classe ordem familia genero especie nome_popular status_conservacao endemismo].freeze

  attr_accessor(*CAMPOS)

  CAMPOS_OBRIGATORIOS = %i[classe ordem familia genero especie].freeze

  validate :campos_obrigatorios_nao_podem_ser_vazios
  validate :endemismo_deve_ser_booleano

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

  private

  def endemismo_deve_ser_booleano
    return unless @chaves_informadas.include?(:endemismo)

    unless [true, false].include?(endemismo)
      errors.add(:endemismo, "deve ser verdadeiro ou falso")
    end
  end

  def campos_obrigatorios_nao_podem_ser_vazios
    (@chaves_informadas & CAMPOS_OBRIGATORIOS).each do |campo|
      if send(campo).blank?
        errors.add(campo, "não pode ficar em branco")
      end
    end
  end
end
