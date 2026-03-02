# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: pearson, pearson_grafico, spearman, spearman_grafico, kendall, kendall_grafico,
    # regressao_linear, modelo_gaussiano, modelo_gaussiano_grafico, modelo_gamma,
    # modelo_gamma_grafico, modelo_poisson, modelo_poisson_grafico,
    # modelo_binomial_negativa, modelo_binomial_grafico
    class CorrelacaoInputDto
      include ActiveModel::API

      attr_accessor :x, :y, :nome_x, :nome_y

      validates :x, presence: true
      validates :y, presence: true

      def initialize(params = {})
        @x = params[:x]
        @y = params[:y]
        @nome_x = params[:nome_x]
        @nome_y = params[:nome_y]
      end

      def to_h
        hash = { x: @x, y: @y }
        hash[:nome_x] = @nome_x if @nome_x.present?
        hash[:nome_y] = @nome_y if @nome_y.present?
        hash
      end
    end
  end
end
