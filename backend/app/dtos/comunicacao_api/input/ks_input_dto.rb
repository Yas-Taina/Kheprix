# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: ks (Kolmogorov-Smirnov)
    # Suporta dois modos:
    # - Duas amostras: amostra1 + amostra2 + labels opcionais
    # - Uma amostra vs distribuição teórica: amostra + distribuicao (ex: "pnorm")
    class KsInputDto
      include ActiveModel::API

      attr_accessor :amostra1, :amostra2, :nome_amostra1, :nome_amostra2,
                    :amostra, :distribuicao

      validate :ao_menos_uma_amostra

      def initialize(params = {})
        @amostra1 = params[:amostra1]
        @amostra2 = params[:amostra2]
        @nome_amostra1 = params[:nome_amostra1]
        @nome_amostra2 = params[:nome_amostra2]
        @amostra = params[:amostra]
        @distribuicao = params[:distribuicao]
      end

      def duas_amostras?
        @amostra2.present?
      end

      def to_h
        if duas_amostras?
          hash = { amostra1: @amostra1, amostra2: @amostra2 }
          hash[:nome_amostra1] = @nome_amostra1 if @nome_amostra1.present?
          hash[:nome_amostra2] = @nome_amostra2 if @nome_amostra2.present?
          hash
        else
          hash = { amostra: @amostra }
          hash[:distribuicao] = @distribuicao if @distribuicao.present?
          hash
        end
      end

      private

      def ao_menos_uma_amostra
        return if @amostra1.present? || @amostra.present?

        errors.add(:base, "Informe amostra1/amostra2 (duas amostras) ou amostra (uma amostra)")
      end
    end
  end
end
