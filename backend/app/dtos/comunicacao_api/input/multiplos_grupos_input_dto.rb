# frozen_string_literal: true

module ComunicacaoApi
  module Input
    class MultiplosGruposInputDto
      include ActiveModel::API

      attr_accessor :valores, :grupos, :nome_variavel

      validates :valores, presence: true
      validates :grupos, presence: true

      def initialize(params = {})
        @valores = params[:valores]
        @grupos = params[:grupos]
        @nome_variavel = params[:nome_variavel]
      end

      def to_h
        hash = { valores: @valores, grupos: @grupos }
        hash[:nome_variavel] = @nome_variavel if @nome_variavel.present?
        hash
      end
    end
  end
end
