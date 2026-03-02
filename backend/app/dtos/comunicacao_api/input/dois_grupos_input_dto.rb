# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: teste_t
    class DoisGruposInputDto
      include ActiveModel::API

      attr_accessor :grupo1, :grupo2, :nome_grupo1, :nome_grupo2

      validates :grupo1, presence: true
      validates :grupo2, presence: true

      def initialize(params = {})
        @grupo1 = params[:grupo1]
        @grupo2 = params[:grupo2]
        @nome_grupo1 = params[:nome_grupo1]
        @nome_grupo2 = params[:nome_grupo2]
      end

      def to_h
        hash = { grupo1: @grupo1, grupo2: @grupo2 }
        hash[:nome_grupo1] = @nome_grupo1 if @nome_grupo1.present?
        hash[:nome_grupo2] = @nome_grupo2 if @nome_grupo2.present?
        hash
      end
    end
  end
end
