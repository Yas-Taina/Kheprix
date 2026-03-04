# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/hurlbert
    class HurlbertResponseDto
      attr_reader :hurlbert_pie, :total_individuos, :interpretacao

      def initialize(dados = {})
        @hurlbert_pie = dados["hurlbert_pie"] || dados[:hurlbert_pie]
        @total_individuos = dados["total_individuos"] || dados[:total_individuos]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
