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

    return { erro: mensagem_sem_dados(analise, params) } unless dados

    dados = adaptar_campos(analise[:chave], dados)

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

    payload = {
      chave: analise[:chave],
      nome: analise[:nome],
      valor: valor,
      grafico: grafico,
      params: params,
      timestamp: Time.zone.now.iso8601
    }

    url_arquivo = salvar_arquivo(payload: payload, estudo_id: estudo_id, chave: analise[:chave], nome: analise[:nome])

    {
      analise: analise[:chave],
      nome: analise[:nome],
      valor: valor,
      grafico: grafico,
      url_arquivo: url_arquivo
    }
  end

  private

  def salvar_arquivo(payload:, estudo_id:, chave:, nome:)
    xml = GenericoHashParaXml.call(payload)
    SalvaResultadoAnalise.salvar(payload: payload, xml: xml, estudo_id: estudo_id, chave: chave, nome: nome)
  rescue StandardError => e
    Rails.logger.warn("Falha ao gravar arquivo de resultado da análise #{chave}: #{e.class}: #{e.message}")
    nil
  end

  def mensagem_sem_dados(analise, params)
    filtros = []
    filtros << "campanha_ids=#{params[:campanha_ids]}" if params[:campanha_ids].present?
    filtros << "data_inicio=#{params[:data_inicio]}" if params[:data_inicio].present?
    filtros << "data_fim=#{params[:data_fim]}" if params[:data_fim].present?
    contexto = filtros.any? ? " com os filtros #{filtros.join(", ")}" : ""
    "Não foi possível montar os dados para '#{analise[:nome]}'#{contexto}. Verifique se existem dados suficientes."
  end

  # A API R usa nomes de campos diferentes para algumas análises.
  # Ex: KS espera amostra1/amostra2 em vez de grupo1/grupo2.
  def adaptar_campos(chave, dados)
    case chave
    when "ks"
      {
        amostra1: dados[:grupo1],
        amostra2: dados[:grupo2],
        nome_amostra1: dados[:nome_grupo1],
        nome_amostra2: dados[:nome_grupo2]
      }.compact
    else
      dados
    end
  end
end
