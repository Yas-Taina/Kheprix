# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/shapiro (Shapiro-Wilk)
    class ShapiroResponseDto
      attr_reader :estatistica_w, :p_valor, :normal, :interpretacao, :nome_variavel, :nota

      def initialize(dados = {})
        @estatistica_w = dados["estatistica_w"] || dados[:estatistica_w]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @normal = dados["normal"] || dados[:normal]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
        @nome_variavel = dados["nome_variavel"] || dados[:nome_variavel]
        @nota = dados["nota"] || dados[:nota]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
