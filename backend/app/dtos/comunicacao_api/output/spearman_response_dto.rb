# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/spearman
    class SpearmanResponseDto
      attr_reader :rho, :p_valor, :significativo, :estatistica_s,
                  :interpretacao, :nome_x, :nome_y

      def initialize(dados = {})
        @rho = dados["rho"] || dados[:rho]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @estatistica_s = dados["estatistica_S"] || dados[:estatistica_S] || dados["estatistica_s"] || dados[:estatistica_s]
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
