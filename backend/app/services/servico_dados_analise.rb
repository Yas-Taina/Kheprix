# frozen_string_literal: true

class ServicoDadosAnalise
  def montar_dados(estudo_id:, tipo_dado:, params:)
    case tipo_dado
    when "abundancias"
      montar_abundancias(estudo_id: estudo_id)
    when "abundancias_por_amostra"
      montar_abundancias_por_amostra(estudo_id: estudo_id)
    when "abundancias_com_variaveis"
      montar_abundancias_com_variaveis(estudo_id: estudo_id, params: params)
    when "dois_vetores"
      montar_dois_vetores(estudo_id: estudo_id, params: params)
    when "dois_grupos"
      montar_dois_grupos(estudo_id: estudo_id, params: params)
    when "multiplos_grupos"
      montar_multiplos_grupos(estudo_id: estudo_id, params: params)
    when "vetor_unico"
      montar_vetor_unico(estudo_id: estudo_id, params: params)
    when "matriz_acumulacao"
      montar_matriz_acumulacao(estudo_id: estudo_id)
    end
  end

  private

  # ==================== Abundâncias ====================

  def montar_abundancias(estudo_id:)
    resultados = registros_unicos(estudo_id)
      .group(:especie)
      .sum(:abundancia)

    resultados.reject! { |_k, v| v <= 0 }
    return nil if resultados.empty?

    abundancias = []
    nomes_especies = []

    resultados.each do |especie_nome, quantidade|
      abundancias << quantidade
      nomes_especies << (especie_nome.presence || "Espécie desconhecida")
    end

    { abundancias: abundancias, nomes_especies: nomes_especies }
  end

  # ==================== Abundâncias por Amostra ====================

  def montar_abundancias_por_amostra(estudo_id:)
    registros = registros_unicos(estudo_id)
      .group(:fk_unidade_amostral, :especie)
      .sum(:abundancia)

    return nil if registros.empty?

    unidades_ids = registros.keys.map(&:first).uniq.sort
    especies_nomes = registros.keys.map(&:last).uniq.sort

    unidades = Dw::DimUnidadeAmostral.where(id_unidade: unidades_ids).index_by(&:id_unidade)
    nomes_amostras = unidades_ids.map { |id| unidades[id]&.metodo_coleta || "Unidade #{id}" }

    matriz = unidades_ids.map do |unidade_id|
      especies_nomes.map do |especie_nome|
        registros[[ unidade_id, especie_nome ]] || 0
      end
    end

    {
      abundancias_por_amostra: matriz,
      nomes_especies: especies_nomes,
      nomes_amostras: nomes_amostras
    }
  end

  # ==================== Abundâncias com Variáveis ====================

  def montar_abundancias_com_variaveis(estudo_id:, params:)
    dados_abundancia = montar_abundancias_por_amostra(estudo_id: estudo_id)
    return nil unless dados_abundancia

    variavel_ids = params[:variavel_ids]
    return nil if variavel_ids.blank?

    var_index = {}
    nome_index = {}

    variavel_ids.each do |vid|
      valores_variavel_unicos(estudo_id, vid).each do |r|
        var_index[[ r.fk_unidade_amostral, r.id_variavel ]] = r.valor_numerico&.to_f || 0.0
        nome_index[r.id_variavel] ||= r.nome_variavel
      end
    end

    unidades_ids = registros_unicos(estudo_id)
      .distinct
      .pluck(:fk_unidade_amostral)
      .sort

    nomes_variaveis_ambientais = variavel_ids.map { |vid| nome_index[vid] || "Variável #{vid}" }

    variaveis_por_amostra = unidades_ids.map do |uid|
      variavel_ids.map { |vid| var_index[[ uid, vid ]] || 0.0 }
    end

    dados_abundancia.merge(
      variaveis_por_amostra: variaveis_por_amostra,
      nomes_variaveis_ambientais: nomes_variaveis_ambientais,
    )
  end

  # ==================== Dois Vetores ====================

  def montar_dois_vetores(estudo_id:, params:)
    variavel_x_id = params[:variavel_x_id]
    variavel_y_id = params[:variavel_y_id]
    return nil if variavel_x_id.blank? || variavel_y_id.blank?

    hash_x = {}
    hash_y = {}
    nome_x = nil
    nome_y = nil

    valores_variavel_unicos(estudo_id, variavel_x_id).each do |r|
      next unless r.valor_numerico
      hash_x[r.fk_unidade_amostral] = r.valor_numerico.to_f
      nome_x ||= r.nome_variavel
    end

    valores_variavel_unicos(estudo_id, variavel_y_id).each do |r|
      next unless r.valor_numerico
      hash_y[r.fk_unidade_amostral] = r.valor_numerico.to_f
      nome_y ||= r.nome_variavel
    end

    ids_comuns = hash_x.keys & hash_y.keys
    return nil if ids_comuns.empty?

    {
      x: ids_comuns.map { |id| hash_x[id] },
      y: ids_comuns.map { |id| hash_y[id] },
      nome_x: nome_x || "Variável X",
      nome_y: nome_y || "Variável Y"
    }
  end

  # ==================== Dois Grupos ====================

  def montar_dois_grupos(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    grupo1_ids = params[:grupo1_ids]
    grupo2_ids = params[:grupo2_ids]
    return nil if grupo1_ids.blank? || grupo2_ids.blank?

    valores_g1 = valores_variavel_por_unidade(
      estudo_id: estudo_id,
      variavel_id: variavel_id,
      unidade_ids: grupo1_ids,
    )
    valores_g2 = valores_variavel_por_unidade(
      estudo_id: estudo_id,
      variavel_id: variavel_id,
      unidade_ids: grupo2_ids,
    )

    return nil if valores_g1.empty? || valores_g2.empty?

    {
      grupo1: valores_g1,
      grupo2: valores_g2,
      nome_grupo1: params[:nome_grupo1] || "Grupo 1",
      nome_grupo2: params[:nome_grupo2] || "Grupo 2"
    }
  end

  # ==================== Múltiplos Grupos ====================

  def montar_multiplos_grupos(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    agrupar_por = params[:agrupar_por] || "unidade_amostral"

    grupo_col = case agrupar_por
    when "campanha" then :fk_campanha
    when "unidade_amostral" then :fk_unidade_amostral
    when "evento_amostragem" then :fk_evento
    else return nil
    end

    rows = valores_variavel_unicos(estudo_id, variavel_id)
    data = rows.map { |r| [ r.send(grupo_col), r.valor_numerico, r.nome_variavel ] }
    return nil if data.empty?

    if agrupar_por == "campanha"
      ids = data.map(&:first).uniq
      labels = base_analises(estudo_id)
        .where(fk_campanha: ids)
        .distinct
        .pluck(:fk_campanha, :nome_campanha)
        .to_h
      label_fn = ->(id) { labels[id] || "Campanha #{id}" }
    elsif agrupar_por == "unidade_amostral"
      ids = data.map(&:first).uniq
      unidades = Dw::DimUnidadeAmostral.where(id_unidade: ids).index_by(&:id_unidade)
      label_fn = ->(id) { unidades[id]&.metodo_coleta || "Unidade #{id}" }
    else
      label_fn = ->(id) { "Evento #{id}" }
    end

    valores = []
    grupos = []
    nome_variavel = nil

    data.each do |grupo_id, valor_num, nome_var|
      next unless valor_num
      nome_variavel ||= nome_var
      valores << valor_num.to_f
      grupos << label_fn.call(grupo_id)
    end

    return nil if valores.empty?

    { valores: valores, grupos: grupos, nome_variavel: nome_variavel || "Variável" }
  end

  # ==================== Vetor Único ====================

  def montar_vetor_unico(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    rows = valores_variavel_unicos(estudo_id, variavel_id)

    dados = []
    nome_variavel = nil

    rows.each do |r|
      next unless r.valor_numerico
      nome_variavel ||= r.nome_variavel
      dados << r.valor_numerico.to_f
    end

    return nil if dados.empty?

    { dados: dados, nome_variavel: nome_variavel || "Variável" }
  end

  # ==================== Matriz de Acumulação ====================

  def montar_matriz_acumulacao(estudo_id:)
    fatos = Dw::FatoMedicaoEntomologica.where(fk_estudo: estudo_id).joins(:tempo)

    registros = fatos
      .select("fato_medicao_entomologica.fk_evento, fato_medicao_entomologica.fk_especie, SUM(fato_medicao_entomologica.quantidade) as total")
      .group("fato_medicao_entomologica.fk_evento, fato_medicao_entomologica.fk_especie")

    return nil if registros.empty?

    eventos_por_data = fatos
      .select("DISTINCT fato_medicao_entomologica.fk_evento, dim_tempo.data_completa")
      .order("dim_tempo.data_completa ASC")

    eventos_ordenados = eventos_por_data.map(&:fk_evento).uniq
    todas_especies = registros.map(&:fk_especie).uniq.sort

    dados_por_evento = {}
    registros.each do |r|
      dados_por_evento[r.fk_evento] ||= {}
      dados_por_evento[r.fk_evento][r.fk_especie] = r.total.to_i
    end

    matriz = eventos_ordenados.map do |evento_id|
      todas_especies.map do |especie_id|
        dados_por_evento.dig(evento_id, especie_id) || 0
      end
    end

    { matriz: matriz }
  end

  # ==================== Helpers ====================

  def base_analises(estudo_id)
    Dw::AnaliseEstatistica.do_estudo(estudo_id)
  end

  def registros_unicos(estudo_id)
    subquery = base_analises(estudo_id)
      .select("DISTINCT ON (id_registro) id_registro, especie, abundancia, fk_unidade_amostral, fk_evento, fk_campanha, nome_campanha")
      .order(:id_registro)
      .to_sql
    Dw::AnaliseEstatistica.from("(#{subquery}) AS analises_estatisticas")
  end

  def valores_variavel_unicos(estudo_id, variavel_id)
    base_analises(estudo_id)
      .where(id_variavel: variavel_id)
      .select("DISTINCT ON (fk_unidade_amostral) fk_unidade_amostral, fk_campanha, nome_campanha, fk_evento, id_variavel, nome_variavel, valor_numerico")
      .order(:fk_unidade_amostral)
  end

  def valores_variavel_por_unidade(estudo_id:, variavel_id:, unidade_ids:)
    valores_variavel_unicos(estudo_id, variavel_id)
      .where(fk_unidade_amostral: unidade_ids)
      .map { |r| r.valor_numerico&.to_f }
      .compact
  end
end
