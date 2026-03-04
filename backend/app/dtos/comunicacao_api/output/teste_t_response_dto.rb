# frozen_string_literal: true

module ComunicacaoApi
  module Output
    # Resposta de: POST /analise/teste_t
    class TesteTResponseDto
      attr_reader :estatistica_t, :p_valor, :significativo, :intervalo_confianca,
                  :media_grupo1, :media_grupo2, :diferenca_medias,
                  :nome_grupo1, :nome_grupo2

      def initialize(dados = {})
        @estatistica_t = dados["estatistica_t"] || dados[:estatistica_t]
        @p_valor = dados["p_valor"] || dados[:p_valor]
        @significativo = dados["significativo"] || dados[:significativo]
        @intervalo_confianca = dados["intervalo_confianca"] || dados[:intervalo_confianca]
        @media_grupo1 = dados["media_grupo1"] || dados[:media_grupo1]
        @media_grupo2 = dados["media_grupo2"] || dados[:media_grupo2]
        @diferenca_medias = dados["diferenca_medias"] || dados[:diferenca_medias]
        @nome_grupo1 = dados["nome_grupo1"] || dados[:nome_grupo1]
        @nome_grupo2 = dados["nome_grupo2"] || dados[:nome_grupo2]
      end

      def self.from_hash(dados)
        new(dados)
      end
    end
  end
end
