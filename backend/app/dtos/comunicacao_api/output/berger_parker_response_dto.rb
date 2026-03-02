# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/berger_parker
    class BergerParkerResponseDto
      attr_reader :indice_berger_parker, :abundancia_maxima, :total_individuos,
                  :proporcao_especie_dominante, :interpretacao

      def initialize(dados = {})
        @indice_berger_parker = dados["indice_berger_parker"] || dados[:indice_berger_parker]
        @abundancia_maxima = dados["abundancia_maxima"] || dados[:abundancia_maxima]
        @total_individuos = dados["total_individuos"] || dados[:total_individuos]
        @proporcao_especie_dominante = dados["proporcao_especie_dominante"] || dados[:proporcao_especie_dominante]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
