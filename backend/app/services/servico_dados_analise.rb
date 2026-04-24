# frozen_string_literal: true

class ServicoDadosAnalise
  def montar_dados(estudo_id:, tipo_dado:, params:)
    @campanha_ids = params[:campanha_ids].presence
    @unidade_ids = params[:unidade_ids].presence
    @evento_ids = params[:evento_ids].presence
    @data_inicio = params[:data_inicio].presence
    @data_fim = params[:data_fim].presence
    @latitude_min = params[:latitude_min]
    @latitude_max = params[:latitude_max]
    @longitude_min = params[:longitude_min]
    @longitude_max = params[:longitude_max]

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

    # Totais agregados zeram/ficam negativos quando todos os registros de uma espécie
    # são ausência/ruído — sem dado útil pra índice. Em análises por amostra/evento
    # o zero é significativo (ausência) e não deve ser removido.
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
    nomes_amostras = unidades_ids.map { |id| unidades[id]&.nome_unidade_amostral || "Unidade #{id}" }

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
      valores_variavel_unicos(estudo_id: estudo_id, variavel_id: vid).each do |r|
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
    fonte_x = (params[:fonte_x] || "variavel").to_s
    fonte_y = (params[:fonte_y] || "variavel").to_s
    nivel = params[:nivel_agregacao] || "unidade_amostral"

    hash_x, nome_x = hash_bivariado(
      estudo_id: estudo_id, fonte: fonte_x,
      variavel_id: params[:variavel_x_id], nivel: nivel,
    )
    hash_y, nome_y = hash_bivariado(
      estudo_id: estudo_id, fonte: fonte_y,
      variavel_id: params[:variavel_y_id], nivel: nivel,
    )
    return nil if hash_x.empty? || hash_y.empty?

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
    fonte = (params[:fonte] || "variavel").to_s
    grupo1_ids = params[:grupo1_ids]
    grupo2_ids = params[:grupo2_ids]
    return nil if grupo1_ids.blank? || grupo2_ids.blank?

    if fonte == "variavel"
      variavel_id = params[:variavel_id]
      return nil if variavel_id.blank?
      valores_g1 = valores_variavel_por_unidade(
        estudo_id: estudo_id, variavel_id: variavel_id, unidade_ids: grupo1_ids,
      )
      valores_g2 = valores_variavel_por_unidade(
        estudo_id: estudo_id, variavel_id: variavel_id, unidade_ids: grupo2_ids,
      )
    else
      por_unidade = dados_derivados(
        estudo_id: estudo_id, fonte: fonte, nivel_agregacao: "unidade_amostral",
      )
      valores_g1 = grupo1_ids.filter_map { |id| por_unidade[id]&.to_f }
      valores_g2 = grupo2_ids.filter_map { |id| por_unidade[id]&.to_f }
    end

    return nil if valores_g1.empty? || valores_g2.empty?

    {
      grupo1: valores_g1,
      grupo2: valores_g2,
      nome_grupo1: params[:nome_grupo1] || "Grupo 1",
      nome_grupo2: params[:nome_grupo2] || "Grupo 2"
    }
  end

  # ==================== Múltiplos Grupos ====================

  AGRUPAMENTOS_VALIDOS = %w[campanha unidade_amostral evento_amostragem mes ano estacao].freeze

  def montar_multiplos_grupos(estudo_id:, params:)
    fonte = (params[:fonte] || "variavel").to_s
    agrupar_por = params[:agrupar_por] || "unidade_amostral"
    return nil unless AGRUPAMENTOS_VALIDOS.include?(agrupar_por)

    rows = if fonte == "variavel"
      variavel_id = params[:variavel_id]
      return nil if variavel_id.blank?
      rows_variavel_para_grupos(estudo_id: estudo_id, variavel_id: variavel_id)
    else
      rows_derivadas_para_grupos(estudo_id: estudo_id, fonte: fonte)
    end
    return nil if rows.empty?

    label_fn = construir_label_fn(estudo_id: estudo_id, agrupar_por: agrupar_por, rows: rows)
    nome_variavel = rows.first[:nome] || "Variável"

    valores = []
    grupos = []

    rows.each do |row|
      next if row[:valor].nil?
      label = label_fn.call(row)
      next if label.nil?
      valores << row[:valor].to_f
      grupos << label
    end

    return nil if valores.empty?

    { valores: valores, grupos: grupos, nome_variavel: nome_variavel }
  end

  # ==================== Vetor Único ====================

  def montar_vetor_unico(estudo_id:, params:)
    fonte = (params[:fonte] || "variavel").to_s

    if fonte == "variavel"
      variavel_id = params[:variavel_id]
      return nil if variavel_id.blank?

      rows = valores_variavel_unicos(estudo_id: estudo_id, variavel_id: variavel_id)
      dados = []
      nome_variavel = nil
      rows.each do |r|
        next unless r.valor_numerico
        nome_variavel ||= r.nome_variavel
        dados << r.valor_numerico.to_f
      end
    else
      nivel = params[:nivel_agregacao] || "unidade_amostral"
      valores = dados_derivados(estudo_id: estudo_id, fonte: fonte, nivel_agregacao: nivel)
      dados = valores.values.map(&:to_f)
      nome_variavel = "#{fonte.capitalize} por #{nivel}"
    end

    return nil if dados.empty?

    { dados: dados, nome_variavel: nome_variavel || "Variável" }
  end

  # ==================== Matriz de Acumulação ====================

  def montar_matriz_acumulacao(estudo_id:)
    base = registros_unicos(estudo_id)

    registros = base
      .group(:fk_evento, :especie)
      .sum(:abundancia)

    return nil if registros.empty?

    eventos_ordenados = base
      .order(:data_registro, :fk_evento)
      .pluck(:fk_evento)
      .uniq

    todas_especies = registros.keys.map(&:last).uniq.sort

    dados_por_evento = {}
    registros.each do |(evento_id, especie_nome), total|
      dados_por_evento[evento_id] ||= {}
      dados_por_evento[evento_id][especie_nome] = total.to_i
    end

    matriz = eventos_ordenados.map do |evento_id|
      todas_especies.map do |especie_nome|
        dados_por_evento.dig(evento_id, especie_nome) || 0
      end
    end

    { matriz: matriz }
  end

  # ==================== Helpers ====================

  def base_analises(estudo_id)
    escopo = Dw::AnaliseEstatistica.do_estudo(estudo_id)
    escopo = escopo.where(fk_campanha: @campanha_ids) if @campanha_ids.present?
    escopo = escopo.where(fk_unidade_amostral: @unidade_ids) if @unidade_ids.present?
    escopo = escopo.where(fk_evento: @evento_ids) if @evento_ids.present?
    if @data_inicio.present? || @data_fim.present?
      escopo = escopo.where(data_registro: @data_inicio..@data_fim)
    end

    ids_bounding_box = unidades_no_bounding_box
    escopo = escopo.where(fk_unidade_amostral: ids_bounding_box) if ids_bounding_box

    escopo
  end

  # Retorna IDs de unidades amostrais dentro do bounding box, ou nil se nenhum
  # parâmetro geográfico foi passado (aí o filtro não se aplica). Consulta o DW
  # (`dim_unidade_amostral`) porque `analises_estatisticas` não tem lat/long.
  def unidades_no_bounding_box
    return nil if @latitude_min.nil? && @latitude_max.nil? &&
                  @longitude_min.nil? && @longitude_max.nil?

    query = Dw::DimUnidadeAmostral.all
    query = query.where("latitude >= ?", @latitude_min) if @latitude_min
    query = query.where("latitude <= ?", @latitude_max) if @latitude_max
    query = query.where("longitude >= ?", @longitude_min) if @longitude_min
    query = query.where("longitude <= ?", @longitude_max) if @longitude_max
    query.pluck(:id_unidade)
  end

  def registros_unicos(estudo_id)
    subquery = base_analises(estudo_id)
      .select("DISTINCT ON (id_registro) id_registro, especie, abundancia, fk_unidade_amostral, fk_evento, fk_campanha, nome_campanha, data_registro, ano, mes")
      .order(:id_registro)
      .to_sql
    Dw::AnaliseEstatistica.from("(#{subquery}) AS analises_estatisticas")
  end

  def valores_variavel_unicos(estudo_id:, variavel_id:)
    coluna = coluna_do_nivel_variavel(estudo_id: estudo_id, variavel_id: variavel_id)

    base_analises(estudo_id)
      .where(id_variavel: variavel_id)
      .select("DISTINCT ON (#{coluna}) id_registro, fk_unidade_amostral, fk_campanha, nome_campanha, fk_evento, id_variavel, nome_variavel, nivel_variavel, valor_numerico")
      .order(coluna)
  end

  def valores_variavel_por_unidade(estudo_id:, variavel_id:, unidade_ids:)
    valores_variavel_unicos(estudo_id: estudo_id, variavel_id: variavel_id)
      .where(fk_unidade_amostral: unidade_ids)
      .map { |r| r.valor_numerico&.to_f }
      .compact
  end

  # Escolhe a coluna do DISTINCT ON conforme o nivel_variavel da variável solicitada.
  # Variáveis CAMPANHA/UNIDADE/EVENTO/REGISTRO precisam ser consultadas na dimensão
  # nativa — normalizar tudo por fk_unidade_amostral perde dados de eventos/registros
  # distintos na mesma unidade e replica valor por unidade no caso de campanha.
  # O retorno é whitelist, então é seguro interpolar no SQL.
  def coluna_do_nivel_variavel(estudo_id:, variavel_id:)
    nivel = base_analises(estudo_id)
      .where(id_variavel: variavel_id)
      .limit(1)
      .pluck(:nivel_variavel)
      .first

    case nivel.to_s.downcase
    when "campanha" then "fk_campanha"
    when "evento"   then "fk_evento"
    when "registro" then "id_registro"
    else "fk_unidade_amostral"
    end
  end

  # ==================== Helpers de Fonte Derivada ====================

  # Retorna hash {chave_dimensao => valor_float} para fonte abundancia/riqueza
  # agrupado pela dimensão escolhida.
  # - abundancia = SUM(abundancia)
  # - riqueza    = COUNT(DISTINCT especie) (exclui ausências com abundancia <= 0)
  def dados_derivados(estudo_id:, fonte:, nivel_agregacao:)
    coluna = coluna_nivel_agregacao(nivel_agregacao)
    base = registros_unicos(estudo_id)

    case fonte
    when "abundancia"
      base.group(coluna).sum(:abundancia)
    when "riqueza"
      base.where("abundancia > 0").group(coluna).distinct.count(:especie)
    else
      {}
    end
  end

  def hash_bivariado(estudo_id:, fonte:, variavel_id:, nivel:)
    if fonte == "variavel"
      return [ {}, nil ] if variavel_id.blank?

      hash = {}
      nome = nil
      valores_variavel_unicos(estudo_id: estudo_id, variavel_id: variavel_id).each do |r|
        next unless r.valor_numerico
        chave = chave_nivel_nativo(r)
        hash[chave] = r.valor_numerico.to_f
        nome ||= r.nome_variavel
      end
      [ hash, nome ]
    else
      valores = dados_derivados(estudo_id: estudo_id, fonte: fonte, nivel_agregacao: nivel)
      [ valores.transform_values(&:to_f), "#{fonte.capitalize} por #{nivel}" ]
    end
  end

  def chave_nivel_nativo(row)
    case row.nivel_variavel.to_s.downcase
    when "campanha" then row.fk_campanha
    when "evento"   then row.fk_evento
    when "registro" then row.id_registro
    else row.fk_unidade_amostral
    end
  end

  def coluna_nivel_agregacao(nivel_agregacao)
    case nivel_agregacao.to_s
    when "campanha" then :fk_campanha
    when "evento"   then :fk_evento
    else :fk_unidade_amostral
    end
  end

  # ==================== Helpers de Múltiplos Grupos ====================

  def rows_variavel_para_grupos(estudo_id:, variavel_id:)
    coluna = coluna_do_nivel_variavel(estudo_id: estudo_id, variavel_id: variavel_id)
    base_analises(estudo_id)
      .where(id_variavel: variavel_id)
      .select(
        "DISTINCT ON (#{coluna}) id_registro, fk_unidade_amostral, fk_campanha, " \
        "nome_campanha, fk_evento, nome_variavel, valor_numerico, ano, mes",
      )
      .order(coluna)
      .map do |r|
        {
          valor: r.valor_numerico,
          nome: r.nome_variavel,
          fk_campanha: r.fk_campanha,
          fk_unidade_amostral: r.fk_unidade_amostral,
          fk_evento: r.fk_evento,
          nome_campanha: r.nome_campanha,
          ano: r.ano,
          mes: r.mes
        }
      end
  end

  def rows_derivadas_para_grupos(estudo_id:, fonte:)
    base = registros_unicos(estudo_id)
    cols = %i[fk_evento fk_unidade_amostral fk_campanha nome_campanha ano mes]

    raw = case fonte
    when "abundancia"
      base.group(*cols).sum(:abundancia)
    when "riqueza"
      base.where("abundancia > 0").group(*cols).distinct.count(:especie)
    else
      {}
    end

    nome = fonte.capitalize
    raw.map do |(evento_id, unidade_id, campanha_id, nome_campanha, ano, mes), valor|
      {
        valor: valor,
        nome: nome,
        fk_campanha: campanha_id,
        fk_unidade_amostral: unidade_id,
        fk_evento: evento_id,
        nome_campanha: nome_campanha,
        ano: ano,
        mes: mes
      }
    end
  end

  def construir_label_fn(estudo_id:, agrupar_por:, rows:)
    case agrupar_por
    when "campanha"
      ->(row) { row[:nome_campanha] || "Campanha #{row[:fk_campanha]}" }
    when "unidade_amostral"
      ids = rows.map { |r| r[:fk_unidade_amostral] }.compact.uniq
      unidades = Dw::DimUnidadeAmostral.where(id_unidade: ids).index_by(&:id_unidade)
      ->(row) { unidades[row[:fk_unidade_amostral]]&.nome_unidade_amostral || "Unidade #{row[:fk_unidade_amostral]}" }
    when "evento_amostragem"
      ->(row) { "Evento #{row[:fk_evento]}" }
    when "ano"
      ->(row) { row[:ano]&.to_s }
    when "mes"
      ->(row) {
        next nil if row[:ano].nil? || row[:mes].nil?
        "#{row[:ano]}-#{row[:mes].to_s.rjust(2, "0")}"
      }
    when "estacao"
      ->(row) { estacao_label(mes: row[:mes], ano: row[:ano]) }
    end
  end

  # Estações do hemisfério sul. O ano da estação é o ano calendário onde
  # janeiro/fevereiro caem (ex.: dezembro/2024 + jan/fev/2025 => "Verão 2025").
  def estacao_label(mes:, ano:)
    return nil if mes.nil? || ano.nil?

    case mes.to_i
    when 12       then "Verão #{ano.to_i + 1}"
    when 1, 2     then "Verão #{ano}"
    when 3, 4, 5  then "Outono #{ano}"
    when 6, 7, 8  then "Inverno #{ano}"
    when 9, 10, 11 then "Primavera #{ano}"
    end
  end
end
