# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/shannon
    class ShannonResponseDto
      attr_reader :indice_shannon, :shannon_maximo, :equitabilidade_relativa,
                  :riqueza, :interpretacao

      def initialize(dados = {})
        @indice_shannon = dados["indice_shannon"] || dados[:indice_shannon]
        @shannon_maximo = dados["shannon_maximo"] || dados[:shannon_maximo]
        @equitabilidade_relativa = dados["equitabilidade_relativa"] || dados[:equitabilidade_relativa]
        @riqueza = dados["riqueza"] || dados[:riqueza]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
