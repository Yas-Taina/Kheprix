# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/anova
    class AnovaResponseDto
      attr_reader :f_valor, :p_valor, :significativo, :gl_tratamento,
                  :gl_residuo, :medias_por_grupo, :nome_variavel, :interpretacao

      def initialize(dados = {})
        @f_valor = dados["f_valor"] || dados[:f_valor]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @gl_tratamento = dados["gl_tratamento"] || dados[:gl_tratamento]
        @gl_residuo = dados["gl_residuo"] || dados[:gl_residuo]
        @medias_por_grupo = dados["medias_por_grupo"] || dados[:medias_por_grupo]
        @nome_variavel = dados["nome_variavel"] || dados[:nome_variavel]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
