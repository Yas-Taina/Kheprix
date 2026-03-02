# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/simpson
    class SimpsonResponseDto
      attr_reader :indice_simpson, :diversidade_simpson, :inverso_simpson, :interpretacao

      def initialize(dados = {})
        @indice_simpson = dados["indice_simpson"] || dados[:indice_simpson]
        @diversidade_simpson = dados["diversidade_simpson"] || dados[:diversidade_simpson]
        @inverso_simpson = dados["inverso_simpson"] || dados[:inverso_simpson]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
