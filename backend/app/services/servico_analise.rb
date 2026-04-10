# frozen_string_literal: true

class ServicoAnalise
  def executar(estudo_id:, chave:, params:)
    analise = CatalogoAnalise.buscar(chave)
    return { erro: "Análise '#{chave}' não encontrada no catálogo" } unless analise

    dados = ServicoDadosAnalise.new.montar_dados(
      estudo_id: estudo_id,
      tipo_dado: analise[:tipo_dado],
      params: params,
    )

    return { erro: "Não foi possível montar os dados para a análise. Verifique se existem dados suficientes no estudo." } unless dados

    cliente = ClienteApiR.new
    valor = nil
    grafico = nil

    if analise[:endpoint_r].present?
      resultado = cliente.executar(endpoint: analise[:endpoint_r], dados: dados)
      valor = resultado[:conteudo] if resultado
    end

    if analise[:endpoint_r_grafico].present?
      resultado = cliente.executar(endpoint: analise[:endpoint_r_grafico], dados: dados)
      grafico = resultado[:conteudo] if resultado
    end

    if valor.nil? && grafico.nil?
      return { erro: "A API R não retornou resultados. Verifique se o serviço está disponível." }
    end

    {
      analise: analise[:chave],
      nome: analise[:nome],
      valor: valor,
      grafico: grafico
    }
  end
end
