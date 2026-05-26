# frozen_string_literal: true

module ComunicacaoApi
  module Input
    class AbundanciasInputDto
      include ActiveModel::API

      attr_accessor :abundancias, :nomes_especies

      validates :abundancias, presence: true

      def initialize(params = {})
        @abundancias = params[:abundancias]
        @nomes_especies = params[:nomes_especies]
      end

      def to_h
        hash = { abundancias: @abundancias }
        hash[:nomes_especies] = @nomes_especies if @nomes_especies.present?
        hash
      end
    end
  end
end
