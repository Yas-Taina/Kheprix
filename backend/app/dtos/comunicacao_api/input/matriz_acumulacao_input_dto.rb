# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: michaelis_menten
    # matriz: linhas = eventos de coleta (amostras em ordem), colunas = espécies
    # Os valores representam abundâncias acumuladas
    class MatrizAcumulacaoInputDto
      include ActiveModel::API

      attr_accessor :matriz

      validates :matriz, presence: true

      def initialize(params = {})
        @matriz = params[:matriz]
      end

      def to_h
        { matriz: @matriz }
      end
    end
  end
end
