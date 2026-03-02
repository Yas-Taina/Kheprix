# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/modelo_poisson
    class GlmPoissonResponseDto
      attr_reader :aic, :deviance, :coeficientes, :sobredispersao,
                  :nome_resposta, :nome_preditor, :familia, :interpretacao, :nota_sobredispersao

      def initialize(dados = {})
        @aic = dados["aic"] || dados[:aic]
        @deviance = dados["deviance"] || dados[:deviance]
        @coeficientes = dados["coeficientes"] || dados[:coeficientes]
        @sobredispersao = dados["sobredispersao"] || dados[:sobredispersao]
        @nome_resposta = dados["nome_resposta"] || dados[:nome_resposta]
        @nome_preditor = dados["nome_preditor"] || dados[:nome_preditor]
        @familia = dados["familia"] || dados[:familia]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
        @nota_sobredispersao = dados["nota_sobredispersao"] || dados[:nota_sobredispersao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
