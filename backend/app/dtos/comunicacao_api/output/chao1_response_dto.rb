# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/chao1
    class Chao1ResponseDto
      attr_reader :riqueza_observada, :riqueza_estimada_chao1, :singletons,
                  :doubletons, :especies_nao_detectadas_estimadas

      def initialize(dados = {})
        @riqueza_observada = dados["riqueza_observada"] || dados[:riqueza_observada]
        @riqueza_estimada_chao1 = dados["riqueza_estimada_chao1"] || dados[:riqueza_estimada_chao1]
        @singletons = dados["singletons"] || dados[:singletons]
        @doubletons = dados["doubletons"] || dados[:doubletons]
        @especies_nao_detectadas_estimadas = dados["especies_nao_detectadas_estimadas"] || dados[:especies_nao_detectadas_estimadas]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
