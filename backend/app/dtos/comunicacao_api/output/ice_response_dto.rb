# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/ice
    class IceResponseDto
      attr_reader :riqueza_observada, :riqueza_estimada_ice,
                  :especies_nao_detectadas_estimadas

      def initialize(dados = {})
        @riqueza_observada = dados["riqueza_observada"] || dados[:riqueza_observada]
        @riqueza_estimada_ice = dados["riqueza_estimada_ice"] || dados[:riqueza_estimada_ice]
        @especies_nao_detectadas_estimadas = dados["especies_nao_detectadas_estimadas"] || dados[:especies_nao_detectadas_estimadas]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
