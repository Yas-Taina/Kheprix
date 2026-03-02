# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/modelo_binomial_negativa
    class GlmBinomialNegativaResponseDto
      attr_reader :aic, :deviance, :coeficientes, :theta,
                  :nome_resposta, :nome_preditor, :familia, :interpretacao

      def initialize(dados = {})
        @aic = dados["aic"] || dados[:aic]
        @deviance = dados["deviance"] || dados[:deviance]
        @coeficientes = dados["coeficientes"] || dados[:coeficientes]
        @theta = dados["theta"] || dados[:theta]
        @nome_resposta = dados["nome_resposta"] || dados[:nome_resposta]
        @nome_preditor = dados["nome_preditor"] || dados[:nome_preditor]
        @familia = dados["familia"] || dados[:familia]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
