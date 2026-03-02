# frozen_string_literal: true

module ComunicacaoApi
  module Input
    # Usado por: lognormal, logserie, geometrica, vara_quebrada, jackknife1, jackknife2,
    # chao1, bootstrap, ace, shannon, simpson, margalef, pielou, berger_parker,
    # brillouin, macintosh, hurlbert, mcnaughton
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
