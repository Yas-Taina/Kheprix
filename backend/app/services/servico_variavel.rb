# frozen_string_literal: true

class ServicoVariavel
  def criar_em_lote(estudo:, variaveis_params:)
    variaveis_params.each do |variavel_params|
      Variavel.create!(
        estudo: estudo,
        nome: variavel_params[:nome],
        metrica: variavel_params[:metrica],
        nivel_aplicacao: variavel_params[:nivel_aplicacao],
        tipo_dado: variavel_params[:tipo_dado],
      )
    end
  end
end
