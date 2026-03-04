# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta genérica para todos os endpoints de gráfico (serializer html)
    # Endpoints: lognormal, logserie, geometrica, vara_quebrada, rarefacao,
    # jaccard_grafico, sorensen_grafico, bray_curtis_grafico, morisita_grafico,
    # pearson_grafico, spearman_grafico, kendall_grafico, regressao_linear,
    # rda, cca, nmds, pca, modelo_gaussiano_grafico, modelo_gamma_grafico,
    # modelo_poisson_grafico, modelo_binomial_grafico, michaelis_menten
    class GraficoResponseDto
      attr_reader :html

      def initialize(html)
        @html = html
      end

      def self.from_response(html_string)
        new(html_string)
      end
    end
  end
end
