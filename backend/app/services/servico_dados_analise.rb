# frozen_string_literal: true

class ServicoDadosAnalise
  def montar_dados(estudo_id:, tipo_dado:, params:)
    case tipo_dado
    when "abundancias"
      montar_abundancias(estudo_id: estudo_id, params: params)
    when "abundancias_por_amostra"
      montar_abundancias_por_amostra(estudo_id: estudo_id, params: params)
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
      montar_matriz_acumulacao(estudo_id: estudo_id, params: params)
    end
  end

  private

  # ==================== DW: Abundâncias ====================

  def montar_abundancias(estudo_id:, params:)
    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)

    resultados = fatos.group(:fk_especie).sum(:quantidade)
    resultados.reject! { |_k, v| v <= 0 }

    return nil if resultados.empty?

    especies = Dw::DimEspecie.where(id_especie: resultados.keys).index_by(&:id_especie)

    abundancias = []
    nomes_especies = []

    resultados.each do |especie_id, quantidade|
      abundancias << quantidade
      especie = especies[especie_id]
      nomes_especies << nome_especie(especie)
    end

    { abundancias: abundancias, nomes_especies: nomes_especies }
  end

  # ==================== DW: Abundâncias por Amostra ====================

  def montar_abundancias_por_amostra(estudo_id:, params:)
    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)

    registros = fatos.group("dim_unidade_amostral.id_unidade", :fk_especie).sum(:quantidade)

    return nil if registros.empty?

    unidades_ids = registros.keys.map(&:first).uniq
    especies_ids = registros.keys.map(&:last).uniq

    unidades = Dw::DimUnidadeAmostral.where(id_unidade: unidades_ids).index_by(&:id_unidade)
    especies = Dw::DimEspecie.where(id_especie: especies_ids).index_by(&:id_especie)

    especies_ordenadas = especies_ids.sort
    nomes_especies = especies_ordenadas.map { |id| nome_especie(especies[id]) }
    nomes_amostras = unidades_ids.sort.map { |id| unidades[id]&.metodo_coleta || "Unidade #{id}" }

    matriz = unidades_ids.sort.map do |unidade_id|
      especies_ordenadas.map do |especie_id|
        registros[[ unidade_id, especie_id ]] || 0
      end
    end

    {
      abundancias_por_amostra: matriz,
      nomes_especies: nomes_especies,
      nomes_amostras: nomes_amostras
    }
  end

  # ==================== DW + OLTP: Abundâncias com Variáveis ====================

  def montar_abundancias_com_variaveis(estudo_id:, params:)
    dados_abundancia = montar_abundancias_por_amostra(estudo_id: estudo_id, params: params)
    return nil unless dados_abundancia

    variavel_ids = params[:variavel_ids]
    return nil if variavel_ids.blank?

    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)

    unidades_ids = fatos.distinct.pluck("dim_unidade_amostral.id_unidade").sort

    # Para cada unidade, pegar UM registro representativo (todos têm os mesmos valores de variável)
    registro_por_unidade = {}
    unidades_ids.each do |unidade_id|
      registro_por_unidade[unidade_id] = fatos
        .where(dim_unidade_amostral: { id_unidade: unidade_id })
        .pick(:id_registro)
    end

    nomes_variaveis_ambientais = variavel_ids.map do |var_id|
      Dw::DimVariavelRegistro
        .where(id_registro: registro_por_unidade.values.compact, id_variavel: var_id)
        .pick(:nome_variavel) || "Variável #{var_id}"
    end

    variaveis_por_amostra = unidades_ids.map do |unidade_id|
      reg_id = registro_por_unidade[unidade_id]
      variavel_ids.map do |var_id|
        Dw::DimVariavelRegistro
          .where(id_registro: reg_id, id_variavel: var_id)
          .pick(:valor_numerico)&.to_f || 0.0
      end
    end

    dados_abundancia.merge(
      variaveis_por_amostra: variaveis_por_amostra,
      nomes_variaveis_ambientais: nomes_variaveis_ambientais,
    )
  end

  # ==================== DW: Dois Vetores ====================

  def montar_dois_vetores(estudo_id:, params:)
    variavel_x_id = params[:variavel_x_id]
    variavel_y_id = params[:variavel_y_id]
    return nil if variavel_x_id.blank? || variavel_y_id.blank?

    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)

    unidades_ids = fatos.distinct.pluck("dim_unidade_amostral.id_unidade").sort

    hash_x = {}
    hash_y = {}

    unidades_ids.each do |unidade_id|
      reg_id = fatos.where(dim_unidade_amostral: { id_unidade: unidade_id }).pick(:id_registro)
      val_x = Dw::DimVariavelRegistro.where(id_registro: reg_id, id_variavel: variavel_x_id).pick(:valor_numerico)
      val_y = Dw::DimVariavelRegistro.where(id_registro: reg_id, id_variavel: variavel_y_id).pick(:valor_numerico)
      hash_x[unidade_id] = val_x if val_x
      hash_y[unidade_id] = val_y if val_y
    end

    ids_comuns = hash_x.keys & hash_y.keys
    return nil if ids_comuns.empty?

    x = ids_comuns.map { |id| hash_x[id].to_f }
    y = ids_comuns.map { |id| hash_y[id].to_f }

    nome_x = Dw::DimVariavelRegistro.where(id_variavel: variavel_x_id).pick(:nome_variavel) || "Variável X"
    nome_y = Dw::DimVariavelRegistro.where(id_variavel: variavel_y_id).pick(:nome_variavel) || "Variável Y"

    { x: x, y: y, nome_x: nome_x, nome_y: nome_y }
  end

  # ==================== DW: Dois Grupos ====================

  def montar_dois_grupos(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    grupo1_ids = params[:grupo1_ids]
    grupo2_ids = params[:grupo2_ids]
    return nil if grupo1_ids.blank? || grupo2_ids.blank?

    valores_g1 = valores_variavel_por_unidade(
      estudo_id: estudo_id, params: params,
      variavel_id: variavel_id, unidade_ids: grupo1_ids,
    )
    valores_g2 = valores_variavel_por_unidade(
      estudo_id: estudo_id, params: params,
      variavel_id: variavel_id, unidade_ids: grupo2_ids,
    )

    return nil if valores_g1.empty? || valores_g2.empty?

    {
      grupo1: valores_g1,
      grupo2: valores_g2,
      nome_grupo1: params[:nome_grupo1] || "Grupo 1",
      nome_grupo2: params[:nome_grupo2] || "Grupo 2"
    }
  end

  # ==================== DW: Múltiplos Grupos ====================

  def montar_multiplos_grupos(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    agrupar_por = params[:agrupar_por] || "unidade_amostral"

    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: { unidade_amostral: :campanha })

    grupo_col, grupo_label = case agrupar_por
    when "campanha"
      [ "dim_campanha.id_campanha", ->(id) {
        Dw::DimCampanha.find_by(id_campanha: id)&.data_inicio.to_s
      } ]
    when "unidade_amostral"
      [ "dim_unidade_amostral.id_unidade", ->(id) {
        ua = Dw::DimUnidadeAmostral.find_by(id_unidade: id)
        ua&.metodo_coleta || "Unidade #{id}"
      } ]
    when "evento_amostragem"
      [ "dim_evento_amostragem.id_evento", ->(id) { "Evento #{id}" } ]
    else
      return nil
    end

    # Para cada grupo, coletar unidades únicas com um registro representativo por unidade
    pares = fatos.pluck(Arel.sql(grupo_col), Arel.sql("dim_unidade_amostral.id_unidade"), :id_registro)
    grupos_unicos = pares.map(&:first).uniq

    return nil if grupos_unicos.empty?

    unidades_por_grupo = {}
    pares.each do |grupo_id, unidade_id, reg_id|
      unidades_por_grupo[grupo_id] ||= {}
      unidades_por_grupo[grupo_id][unidade_id] ||= reg_id
    end

    valores = []
    grupos = []
    nome_variavel = nil

    unidades_por_grupo.each do |grupo_id, unidades_reg|
      nome_grupo = grupo_label.call(grupo_id)
      unidades_reg.each do |_unidade_id, reg_id|
        var = Dw::DimVariavelRegistro
          .find_by(id_registro: reg_id, id_variavel: variavel_id)

        next unless var&.valor_numerico

        nome_variavel ||= var.nome_variavel
        valores << var.valor_numerico.to_f
        grupos << nome_grupo
      end
    end

    return nil if valores.empty?

    { valores: valores, grupos: grupos, nome_variavel: nome_variavel || "Variável" }
  end

  # ==================== DW: Vetor Único ====================

  def montar_vetor_unico(estudo_id:, params:)
    variavel_id = params[:variavel_id]
    return nil if variavel_id.blank?

    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)

    unidades_ids = fatos.distinct.pluck("dim_unidade_amostral.id_unidade").sort

    dados = []
    nome_variavel = nil

    unidades_ids.each do |unidade_id|
      reg_id = fatos.where(dim_unidade_amostral: { id_unidade: unidade_id }).pick(:id_registro)
      var = Dw::DimVariavelRegistro.find_by(id_registro: reg_id, id_variavel: variavel_id)
      next unless var&.valor_numerico

      nome_variavel ||= var.nome_variavel
      dados << var.valor_numerico.to_f
    end

    return nil if dados.empty?

    { dados: dados, nome_variavel: nome_variavel || "Variável" }
  end

  # ==================== DW: Matriz de Acumulação ====================

  def montar_matriz_acumulacao(estudo_id:, params:)
    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(:tempo, evento: :unidade_amostral)

    registros = fatos
      .select("fato_medicao_entomologica.fk_evento, fato_medicao_entomologica.fk_especie, SUM(fato_medicao_entomologica.quantidade) as total")
      .group("fato_medicao_entomologica.fk_evento, fato_medicao_entomologica.fk_especie")

    return nil if registros.empty?

    eventos_por_data = fatos
      .joins(:tempo)
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

  def base_fatos_dw(estudo_id:, params:)
    fatos = Dw::FatoMedicaoEntomologica.where(fk_estudo: estudo_id)

    if params[:filtro_data_inicio].present? || params[:filtro_data_fim].present?
      fatos = fatos.joins(:tempo)
      fatos = fatos.where("dim_tempo.data_completa >= ?", params[:filtro_data_inicio]) if params[:filtro_data_inicio].present?
      fatos = fatos.where("dim_tempo.data_completa <= ?", params[:filtro_data_fim]) if params[:filtro_data_fim].present?
    end

    if params[:filtro_campanhas].present?
      fatos = fatos.joins(evento: { unidade_amostral: :campanha })
        .where(dim_campanha: { id_campanha: params[:filtro_campanhas] })
    end

    fatos
  end

  def valores_variavel_por_unidade(estudo_id:, params:, variavel_id:, unidade_ids:)
    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)
      .where(dim_unidade_amostral: { id_unidade: unidade_ids })

    unidades = fatos.distinct.pluck("dim_unidade_amostral.id_unidade")

    unidades.filter_map do |unidade_id|
      reg_id = fatos.where(dim_unidade_amostral: { id_unidade: unidade_id }).pick(:id_registro)
      Dw::DimVariavelRegistro
        .where(id_registro: reg_id, id_variavel: variavel_id)
        .pick(:valor_numerico)&.to_f
    end
  end

  def nome_especie(especie)
    return "Espécie desconhecida" unless especie

    especie.nome_cientifico.presence || especie.nome_popular.presence || "Espécie #{especie.id_especie}"
  end
end
