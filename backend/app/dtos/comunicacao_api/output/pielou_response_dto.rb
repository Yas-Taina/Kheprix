# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/pielou
    class PielouResponseDto
      attr_reader :indice_pielou, :shannon, :riqueza, :interpretacao

      def initialize(dados = {})
        @indice_pielou = dados["indice_pielou"] || dados[:indice_pielou]
        @shannon = dados["shannon"] || dados[:shannon]
        @riqueza = dados["riqueza"] || dados[:riqueza]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
