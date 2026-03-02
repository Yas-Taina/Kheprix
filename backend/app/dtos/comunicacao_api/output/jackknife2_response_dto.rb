# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/jackknife2
    class Jackknife2ResponseDto
      attr_reader :riqueza_observada, :riqueza_estimada_jack2, :especies_unicas,
                  :especies_duplicadas, :especies_nao_detectadas_estimadas

      def initialize(dados = {})
        @riqueza_observada = dados["riqueza_observada"] || dados[:riqueza_observada]
        @riqueza_estimada_jack2 = dados["riqueza_estimada_jack2"] || dados[:riqueza_estimada_jack2]
        @especies_unicas = dados["especies_unicas"] || dados[:especies_unicas]
        @especies_duplicadas = dados["especies_duplicadas"] || dados[:especies_duplicadas]
        @especies_nao_detectadas_estimadas = dados["especies_nao_detectadas_estimadas"] || dados[:especies_nao_detectadas_estimadas]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
