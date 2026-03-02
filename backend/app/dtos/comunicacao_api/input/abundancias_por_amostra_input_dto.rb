# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: chao2, ice, jaccard, jaccard_grafico, sorensen, sorensen_grafico,
    # bray_curtis, bray_curtis_grafico, morisita, morisita_grafico, nmds, pca
    class AbundanciasPorAmostraInputDto
      include ActiveModel::API

      attr_accessor :abundancias_por_amostra, :nomes_especies, :nomes_amostras

      validates :abundancias_por_amostra, presence: true
      validates :nomes_especies, presence: true
      validates :nomes_amostras, presence: true

      def initialize(params = {})
        @abundancias_por_amostra = params[:abundancias_por_amostra]
        @nomes_especies = params[:nomes_especies]
        @nomes_amostras = params[:nomes_amostras]
      end

      def to_h
        {
          abundancias_por_amostra: @abundancias_por_amostra,
          nomes_especies: @nomes_especies,
          nomes_amostras: @nomes_amostras
        }
      end
    end
  end
end
