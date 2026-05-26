# frozen_string_literal: true

module ComunicacaoApi
  module Output
    class KsResponseDto
      attr_reader :estatistica_d, :p_valor, :significativo, :interpretacao,
                  :nome_amostra1, :nome_amostra2

      def initialize(dados = {})
        @estatistica_d = dados["estatistica_d"] || dados[:estatistica_d]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
        @nome_amostra1 = dados["nome_amostra1"] || dados[:nome_amostra1]
        @nome_amostra2 = dados["nome_amostra2"] || dados[:nome_amostra2]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
