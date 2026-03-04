# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/kruskal (Kruskal-Wallis)
    class KruskalResponseDto
      attr_reader :estatistica_h, :p_valor, :significativo, :gl,
                  :medianas_por_grupo, :nome_variavel, :interpretacao

      def initialize(dados = {})
        @estatistica_h = dados["estatistica_h"] || dados[:estatistica_h]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @gl = dados["gl"] || dados[:gl]
        @medianas_por_grupo = dados["medianas_por_grupo"] || dados[:medianas_por_grupo]
        @nome_variavel = dados["nome_variavel"] || dados[:nome_variavel]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
