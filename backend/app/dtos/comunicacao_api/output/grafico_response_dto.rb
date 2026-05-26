# frozen_string_literal: true

module ComunicacaoApi
  module Output
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
