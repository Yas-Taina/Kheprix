# frozen_string_literal: true

module ComunicacaoApi
  module Input
    class MatrizAcumulacaoInputDto
      include ActiveModel::API

      attr_accessor :matriz

      validates :matriz, presence: true

      def initialize(params = {})
        @matriz = params[:matriz]
      end

      def to_h
        { matriz: @matriz }
      end
    end
  end
end
