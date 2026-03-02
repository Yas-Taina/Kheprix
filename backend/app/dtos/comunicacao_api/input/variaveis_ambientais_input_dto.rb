# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: rda, cca
    # Requer mais amostras do que variáveis ambientais
    class VariaveisAmbientaisInputDto
      include ActiveModel::API

      attr_accessor :abundancias_por_amostra, :variaveis_por_amostra,
                    :nomes_especies, :nomes_amostras, :nomes_variaveis_ambientais

      validates :abundancias_por_amostra, presence: true
      validates :variaveis_por_amostra, presence: true
      validates :nomes_especies, presence: true
      validates :nomes_amostras, presence: true
      validates :nomes_variaveis_ambientais, presence: true

      def initialize(params = {})
        @abundancias_por_amostra = params[:abundancias_por_amostra]
        @variaveis_por_amostra = params[:variaveis_por_amostra]
        @nomes_especies = params[:nomes_especies]
        @nomes_amostras = params[:nomes_amostras]
        @nomes_variaveis_ambientais = params[:nomes_variaveis_ambientais]
      end

      def to_h
        {
          abundancias_por_amostra: @abundancias_por_amostra,
          variaveis_por_amostra: @variaveis_por_amostra,
          nomes_especies: @nomes_especies,
          nomes_amostras: @nomes_amostras,
          nomes_variaveis_ambientais: @nomes_variaveis_ambientais
        }
      end
    end
  end
end
