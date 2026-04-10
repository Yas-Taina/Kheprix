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

    variaveis = Variavel.where(id: variavel_ids, estudo_id: estudo_id)
    return nil if variaveis.empty?

    fatos = base_fatos_dw(estudo_id: estudo_id, params: params)
      .joins(evento: :unidade_amostral)

    unidades_ids = fatos.select("DISTINCT dim_unidade_amostral.id_unidade")
      .pluck("dim_unidade_amostral.id_unidade").sort

    variaveis_por_amostra = unidades_ids.map do |unidade_id|
      variaveis.map do |variavel|
        valor = ValorVariavel.find_by(variavel_id: variavel.id, id_nivel_aplicacao: unidade_id)
        valor&.valor&.to_f || 0.0
      end
    end

    nomes_variaveis_ambientais = variaveis.map(&:nome)

    dados_abundancia.merge(
      variaveis_por_amostra: variaveis_por_amostra,
      nomes_variaveis_ambientais: nomes_variaveis_ambientais,
    )
  end

  # ==================== OLTP: Dois Vetores ====================

  def montar_dois_vetores(estudo_id:, params:)
    variavel_x = Variavel.find_by(id: params[:variavel_x_id], estudo_id: estudo_id)
    variavel_y = Variavel.find_by(id: params[:variavel_y_id], estudo_id: estudo_id)
    return nil unless variavel_x && variavel_y

    valores_x = ValorVariavel.where(variavel_id: variavel_x.id)
      .joins(:variavel).where(variaveis: { estudo_id: estudo_id })
    valores_y = ValorVariavel.where(variavel_id: variavel_y.id)
      .joins(:variavel).where(variaveis: { estudo_id: estudo_id })

    valores_x = filtrar_valores_variaveis(valores_x, params: params, estudo_id: estudo_id)
    valores_y = filtrar_valores_variaveis(valores_y, params: params, estudo_id: estudo_id)

    hash_x = valores_x.pluck(:id_nivel_aplicacao, :valor).to_h
    hash_y = valores_y.pluck(:id_nivel_aplicacao, :valor).to_h

    ids_comuns = hash_x.keys & hash_y.keys
    return nil if ids_comuns.empty?

    x = ids_comuns.map { |id| hash_x[id].to_f }
    y = ids_comuns.map { |id| hash_y[id].to_f }

    { x: x, y: y, nome_x: variavel_x.nome, nome_y: variavel_y.nome }
  end

  # ==================== OLTP: Dois Grupos ====================

  def montar_dois_grupos(estudo_id:, params:)
    variavel = Variavel.find_by(id: params[:variavel_id], estudo_id: estudo_id)
    return nil unless variavel

    grupo1_ids = params[:grupo1_ids]
    grupo2_ids = params[:grupo2_ids]
    return nil if grupo1_ids.blank? || grupo2_ids.blank?

    valores_g1 = ValorVariavel.where(variavel_id: variavel.id, id_nivel_aplicacao: grupo1_ids)
      .pluck(:valor).map(&:to_f)
    valores_g2 = ValorVariavel.where(variavel_id: variavel.id, id_nivel_aplicacao: grupo2_ids)
      .pluck(:valor).map(&:to_f)

    return nil if valores_g1.empty? || valores_g2.empty?

    {
      grupo1: valores_g1,
      grupo2: valores_g2,
      nome_grupo1: params[:nome_grupo1] || "Grupo 1",
      nome_grupo2: params[:nome_grupo2] || "Grupo 2"
    }
  end

  # ==================== OLTP: Múltiplos Grupos ====================

  def montar_multiplos_grupos(estudo_id:, params:)
    variavel = Variavel.find_by(id: params[:variavel_id], estudo_id: estudo_id)
    return nil unless variavel

    agrupar_por = params[:agrupar_por] || "unidade_amostral"

    ids_por_grupo = obter_ids_por_grupo(estudo_id: estudo_id, agrupar_por: agrupar_por, params: params)
    return nil if ids_por_grupo.empty?

    valores = []
    grupos = []

    ids_por_grupo.each do |nome_grupo, ids|
      vals = ValorVariavel.where(variavel_id: variavel.id, id_nivel_aplicacao: ids)
        .pluck(:valor).map(&:to_f)

      vals.each do |v|
        valores << v
        grupos << nome_grupo
      end
    end

    return nil if valores.empty?

    { valores: valores, grupos: grupos, nome_variavel: variavel.nome }
  end

  # ==================== OLTP: Vetor Único ====================

  def montar_vetor_unico(estudo_id:, params:)
    variavel = Variavel.find_by(id: params[:variavel_id], estudo_id: estudo_id)
    return nil unless variavel

    valores = ValorVariavel.where(variavel_id: variavel.id)
      .joins(:variavel).where(variaveis: { estudo_id: estudo_id })

    valores = filtrar_valores_variaveis(valores, params: params, estudo_id: estudo_id)

    dados = valores.pluck(:valor).map(&:to_f)
    return nil if dados.empty?

    { dados: dados, nome_variavel: variavel.nome }
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

  def filtrar_valores_variaveis(valores, params:, estudo_id:)
    return valores unless params[:filtro_campanhas].present?

    nivel_ids = ids_do_escopo_estudo(estudo_id: estudo_id, params: params)
    valores.where(id_nivel_aplicacao: nivel_ids) if nivel_ids.present?
    valores
  end

  def ids_do_escopo_estudo(estudo_id:, params:)
    campanhas = Campanha.where(estudo_id: estudo_id)
    campanhas = campanhas.where(id: params[:filtro_campanhas]) if params[:filtro_campanhas].present?

    unidades = UnidadeAmostral.where(campanha_id: campanhas.select(:id))
    eventos = EventoAmostragem.where(unidade_amostral_id: unidades.select(:id))

    campanhas.pluck(:id) + unidades.pluck(:id) + eventos.pluck(:id)
  end

  def obter_ids_por_grupo(estudo_id:, agrupar_por:, params:)
    campanhas = Campanha.where(estudo_id: estudo_id)
    campanhas = campanhas.where(id: params[:filtro_campanhas]) if params[:filtro_campanhas].present?

    case agrupar_por
    when "campanha"
      campanhas.each_with_object({}) do |c, hash|
        hash[c.nome] = [ c.id ]
      end
    when "unidade_amostral"
      unidades = UnidadeAmostral.where(campanha_id: campanhas.select(:id))
      unidades.each_with_object({}) do |u, hash|
        hash[u.nome] = [ u.id ]
      end
    when "evento_amostragem"
      unidades = UnidadeAmostral.where(campanha_id: campanhas.select(:id))
      eventos = EventoAmostragem.where(unidade_amostral_id: unidades.select(:id))
      eventos.each_with_object({}) do |e, hash|
        hash["Evento #{e.id}"] = [ e.id ]
      end
    else
      {}
    end
  end

  def nome_especie(especie)
    return "Espécie desconhecida" unless especie

    especie.nome_cientifico.presence || especie.nome_popular.presence || "Espécie #{especie.id_especie}"
  end
end
