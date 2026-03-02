# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/chao2
    class Chao2ResponseDto
      attr_reader :riqueza_observada, :riqueza_estimada_chao2, :especies_unicas,
                  :especies_duplicadas, :numero_amostras, :especies_nao_detectadas_estimadas

      def initialize(dados = {})
        @riqueza_observada = dados["riqueza_observada"] || dados[:riqueza_observada]
        @riqueza_estimada_chao2 = dados["riqueza_estimada_chao2"] || dados[:riqueza_estimada_chao2]
        @especies_unicas = dados["especies_unicas"] || dados[:especies_unicas]
        @especies_duplicadas = dados["especies_duplicadas"] || dados[:especies_duplicadas]
        @numero_amostras = dados["numero_amostras"] || dados[:numero_amostras]
        @especies_nao_detectadas_estimadas = dados["especies_nao_detectadas_estimadas"] || dados[:especies_nao_detectadas_estimadas]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
