# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: shapiro (Shapiro-Wilk)
    # Aceita no máximo 5000 observações
    class DadosSimplesInputDto
      include ActiveModel::API

      attr_accessor :dados, :nome_variavel

      validates :dados, presence: true

      def initialize(params = {})
        @dados = params[:dados]
        @nome_variavel = params[:nome_variavel]
      end

      def to_h
        hash = { dados: @dados }
        hash[:nome_variavel] = @nome_variavel if @nome_variavel.present?
        hash
      end
    end
  end
end
