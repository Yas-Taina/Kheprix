# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/pearson
    class PearsonResponseDto
      attr_reader :correlacao, :p_valor, :significativo, :intervalo_confianca,
                  :interpretacao, :direcao, :nome_x, :nome_y

      def initialize(dados = {})
        @correlacao = dados["correlacao"] || dados[:correlacao]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @intervalo_confianca = dados["intervalo_confianca"] || dados[:intervalo_confianca]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
        @direcao = dados["direcao"] || dados[:direcao]
        @nome_x = dados["nome_x"] || dados[:nome_x]
        @nome_y = dados["nome_y"] || dados[:nome_y]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
