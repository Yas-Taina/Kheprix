# frozen_string_literal: true

class ServicoAnalise
  # Requisitos estatisticos da variavel resposta (y) por familia GLM. A API R
  # faz as.integer(y) silencioso pra Poisson/Binomial Negativa — sem este
  # pre-check, o usuario poderia mandar uma variavel continua (pH, temperatura)
  # como y e receber um modelo ajustado em valores truncados, estatisticamente
  # incorreto, sem nenhum aviso. Pra Gamma a R fala "non-positive values not
  # allowed" mas em ingles e abaixo da camada de servico — pre-check aqui devolve
  # mensagem em PT antes de chegar nela.
  GLM_REQUISITOS_Y = {
    "modelo_poisson"           => :contagens,
    "modelo_binomial_negativa" => :contagens,
    "modelo_gamma"             => :positivos,
  }.freeze

  def executar(estudo_id:, chave:, params:)
    analise = CatalogoAnalise.buscar(chave)
    return { erro: "Análise '#{chave}' não encontrada no catálogo" } unless analise

    begin
      dados = ServicoDadosAnalise.new.montar_dados(
        estudo_id: estudo_id,
        tipo_dado: analise[:tipo_dado],
        params: params,
      )
      validar_compatibilidade_glm!(analise, dados)
    rescue ServicoDadosAnalise::NivelIncompativel => e
      return { erro: e.message }
    rescue ServicoDadosAnalise::DadosDegenerados => e
      return { erro: e.message }
    end

    return { erro: mensagem_sem_dados(analise, params) } unless dados

    dados = adaptar_campos(analise[:chave], dados)

    cliente = ClienteApiR.new
    valor_resultado = chamar_endpoint(cliente, analise[:endpoint_r], dados, analise[:chave])
    grafico_resultado = chamar_endpoint(cliente, analise[:endpoint_r_grafico], dados, analise[:chave])

    valor = valor_resultado&.dig(:conteudo)
    grafico = grafico_resultado&.dig(:conteudo)

    if falhou_completamente?(analise, valor_resultado, grafico_resultado)
      return { erro: mensagem_falha_completa(analise, valor_resultado, grafico_resultado) }
    end

    aviso = montar_aviso_parcial(analise, valor_resultado, grafico_resultado)

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
      url_arquivo: url_arquivo,
      aviso: aviso
    }.compact
  end

  private

  def validar_compatibilidade_glm!(analise, dados)
    requisito = GLM_REQUISITOS_Y[analise[:chave]]
    return unless requisito
    return unless dados.is_a?(Hash) && dados[:y].is_a?(Array)

    y = dados[:y]
    nome_y = dados[:nome_y].presence || "Y"

    case requisito
    when :contagens
      invalido = y.find { |v| !(v.is_a?(Numeric) && v >= 0 && v == v.to_i) }
      if invalido
        raise ServicoDadosAnalise::DadosDegenerados,
              "#{analise[:nome]} exige contagens (inteiros não negativos) em '#{nome_y}'; " \
              "encontrou valor incompatível #{invalido}. Use uma variável de contagem " \
              "(número de indivíduos) ou fonte_y=abundancia."
      end
    when :positivos
      invalido = y.find { |v| !(v.is_a?(Numeric) && v > 0) }
      if invalido
        raise ServicoDadosAnalise::DadosDegenerados,
              "#{analise[:nome]} exige valores estritamente positivos em '#{nome_y}'; " \
              "encontrou valor incompatível #{invalido}. Use uma variável sem zeros nem negativos."
      end
    end
  end

  def chamar_endpoint(cliente, endpoint, dados, chave)
    return nil if endpoint.blank?
    cliente.executar(endpoint: endpoint, dados: dados)
  end

  def falhou_completamente?(analise, valor_resultado, grafico_resultado)
    if analise[:endpoint_r].present? && analise[:endpoint_r_grafico].present?
      !sucesso_valor?(valor_resultado) && !sucesso_grafico?(grafico_resultado)
    elsif analise[:endpoint_r].present?
      !sucesso_valor?(valor_resultado)
    elsif analise[:endpoint_r_grafico].present?
      !sucesso_grafico?(grafico_resultado)
    else
      true
    end
  end

  def sucesso_valor?(resultado)
    resultado.is_a?(Hash) && resultado[:status] == ClienteApiR::STATUS_OK && !resultado[:conteudo].nil?
  end
  alias_method :sucesso_grafico?, :sucesso_valor?

  def mensagem_falha_completa(analise, valor_resultado, grafico_resultado)
    falhas = [ valor_resultado, grafico_resultado ].compact
    indisponivel = falhas.any? { |r| r[:status] == ClienteApiR::STATUS_INDISPONIVEL }

    if indisponivel
      "A API R está indisponível no momento. Tente novamente em alguns instantes."
    else
      mensagens = falhas.filter_map { |r| r[:mensagem_erro] }.uniq.join(" | ")
      "A API R não conseguiu processar a análise '#{analise[:nome]}': #{mensagens}. " \
      "Os dados podem ser inadequados pra esse método (variância zero, matriz singular, etc.)."
    end
  end

  def montar_aviso_parcial(analise, valor_resultado, grafico_resultado)
    return nil unless analise[:endpoint_r].present? && analise[:endpoint_r_grafico].present?

    if !sucesso_valor?(valor_resultado) && sucesso_grafico?(grafico_resultado)
      "Valor numérico não foi calculado: #{descricao_falha(valor_resultado)}."
    elsif sucesso_valor?(valor_resultado) && !sucesso_grafico?(grafico_resultado)
      "Gráfico não foi gerado: #{descricao_falha(grafico_resultado)}."
    end
  end

  def descricao_falha(resultado)
    case resultado&.dig(:status)
    when ClienteApiR::STATUS_INDISPONIVEL
      "API R indisponível"
    when ClienteApiR::STATUS_ERRO_R
      resultado[:mensagem_erro].presence || "API R retornou erro"
    else
      "endpoint não retornou conteúdo"
    end
  end

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
    filtros << "unidade_ids=#{params[:unidade_ids]}" if params[:unidade_ids].present?
    filtros << "evento_ids=#{params[:evento_ids]}" if params[:evento_ids].present?
    filtros << "data_inicio=#{params[:data_inicio]}" if params[:data_inicio].present?
    filtros << "data_fim=#{params[:data_fim]}" if params[:data_fim].present?
    filtros << "latitude_min=#{params[:latitude_min]}" if params[:latitude_min].present?
    filtros << "latitude_max=#{params[:latitude_max]}" if params[:latitude_max].present?
    filtros << "longitude_min=#{params[:longitude_min]}" if params[:longitude_min].present?
    filtros << "longitude_max=#{params[:longitude_max]}" if params[:longitude_max].present?

    if filtros.any?
      "Nenhum dado foi encontrado para os filtros aplicados em '#{analise[:nome]}': " \
      "#{filtros.join(", ")}. Tente ampliar o intervalo de datas ou remover algum filtro."
    else
      "O estudo não tem dados suficientes para '#{analise[:nome]}'. " \
      "Verifique se há registros de ocorrência cadastrados."
    end
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
