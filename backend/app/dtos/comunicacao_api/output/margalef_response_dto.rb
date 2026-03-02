# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/margalef
    class MargalefResponseDto
      attr_reader :indice_margalef, :riqueza, :total_individuos, :interpretacao

      def initialize(dados = {})
        @indice_margalef = dados["indice_margalef"] || dados[:indice_margalef]
        @riqueza = dados["riqueza"] || dados[:riqueza]
        @total_individuos = dados["total_individuos"] || dados[:total_individuos]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
