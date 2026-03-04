# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/mcnaughton
    class McNaughtonResponseDto
      attr_reader :indice_mcnaughton, :proporcao_duas_especies_dominantes,
                  :especie_mais_abundante, :abundancia_primeira,
                  :segunda_mais_abundante, :abundancia_segunda, :interpretacao

      def initialize(dados = {})
        @indice_mcnaughton = dados["indice_mcnaughton"] || dados[:indice_mcnaughton]
        @proporcao_duas_especies_dominantes = dados["proporcao_duas_especies_dominantes"] || dados[:proporcao_duas_especies_dominantes]
        @especie_mais_abundante = dados["especie_mais_abundante"] || dados[:especie_mais_abundante]
        @abundancia_primeira = dados["abundancia_primeira"] || dados[:abundancia_primeira]
        @segunda_mais_abundante = dados["segunda_mais_abundante"] || dados[:segunda_mais_abundante]
        @abundancia_segunda = dados["abundancia_segunda"] || dados[:abundancia_segunda]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
