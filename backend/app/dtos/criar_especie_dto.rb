# frozen_string_literal: true

class CriarEspecieDto
  include ActiveModel::API

  CAMPOS = %i[foto classe ordem familia genero especie nome_popular status_conservacao endemismo].freeze

  attr_accessor(*CAMPOS)

  validates :classe, :ordem, :familia, :genero, :especie, presence: { message: "não pode ficar em branco" }
  validates :endemismo, inclusion: { in: [true, false], message: "deve ser verdadeiro ou falso" }

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
