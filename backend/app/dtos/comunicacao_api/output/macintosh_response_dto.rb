# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/macintosh
    class MacintoshResponseDto
      attr_reader :indice_macintosh, :distancia_u, :total_individuos, :interpretacao

      def initialize(dados = {})
        @indice_macintosh = dados["indice_macintosh"] || dados[:indice_macintosh]
        @distancia_u = dados["distancia_U"] || dados[:distancia_U] || dados["distancia_u"] || dados[:distancia_u]
        @total_individuos = dados["total_individuos"] || dados[:total_individuos]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
