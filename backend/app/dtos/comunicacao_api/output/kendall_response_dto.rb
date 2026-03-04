# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/kendall
    class KendallResponseDto
      attr_reader :tau, :p_valor, :significativo, :estatistica_z,
                  :interpretacao, :nome_x, :nome_y

      def initialize(dados = {})
        @tau = dados["tau"] || dados[:tau]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @estatistica_z = dados["estatistica_z"] || dados[:estatistica_z]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
        @nome_x = dados["nome_x"] || dados[:nome_x]
        @nome_y = dados["nome_y"] || dados[:nome_y]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
