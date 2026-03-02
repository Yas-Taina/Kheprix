# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta genérica para os índices de similaridade/dissimilaridade
    # Endpoints: jaccard, sorensen, bray_curtis, morisita
    class MatrizDistanciaResponseDto
      attr_reader :matriz_distancia, :nomes_amostras, :numero_amostras,
                  :numero_especies, :metodo, :interpretacao

      def initialize(dados = {})
        @matriz_distancia = dados["matriz_distancia"] || dados[:matriz_distancia]
        @nomes_amostras = dados["nomes_amostras"] || dados[:nomes_amostras]
        @numero_amostras = dados["numero_amostras"] || dados[:numero_amostras]
        @numero_especies = dados["numero_especies"] || dados[:numero_especies]
        @metodo = dados["metodo"] || dados[:metodo]
        @interpretacao = dados["interpretacao"] || dados[:interpretacao]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
