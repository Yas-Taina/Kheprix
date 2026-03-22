# frozen_string_literal: true

class AlterarPerfilColaboradorDto
  include ActiveModel::API

  attr_accessor :perfil

  PERFIS_VALIDOS = %w[colaborador proprietario].freeze

  validates :perfil, presence: true, inclusion: { in: PERFIS_VALIDOS }

  def initialize(params = {})
    @perfil = params[:perfil]
  end
end
