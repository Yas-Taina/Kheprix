# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/brillouin
    class BrillouinResponseDto
      attr_reader :indice_brillouin, :total_individuos, :interpretacao

      def initialize(dados = {})
        @indice_brillouin = dados["indice_brillouin"] || dados[:indice_brillouin]
        @total_individuos = dados["total_individuos"] || dados[:total_individuos]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
