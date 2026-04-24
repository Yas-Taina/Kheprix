# frozen_string_literal: true

class ServicoDashboardEstudo
  def dados_completos(estudo:)
    indicadores = Dw::IndicadoresDashboard.do_estudo(estudo.id)

    {
      estudo: { id: estudo.id, nome: estudo.nome, updated_at: estudo.updated_at },
      resumo: resumo(indicadores),
      registros_por_data: registros_por_data(indicadores),
      ocorrencias_por_especie: ocorrencias_por_especie(indicadores),
      pontos_mapa: pontos_mapa(indicadores),
      registros_por_especie_tempo: registros_por_especie_tempo(indicadores),
      especies_distintas_por_mes: especies_distintas_por_mes(indicadores)
    }
  end

  private

  def resumo(indicadores)
    {
      total_registros: indicadores.count,
      total_especies: indicadores.distinct.count(:nome_cientifico),
      especies_ameacadas: indicadores.where(is_ameacada: true).distinct.count(:nome_cientifico),
      especies_nativas: indicadores.where(is_endemica: true).distinct.count(:nome_cientifico),
      especies_invasoras: indicadores.where(is_endemica: false).distinct.count(:nome_cientifico),
      total_individuos: indicadores.sum(:quantidade),
      data_inicio: indicadores.minimum(:data_inicio_campanha)
    }
  end

  def registros_por_data(indicadores)
    indicadores
      .group(:data_registro)
      .count
      .sort_by { |data, _| data }
      .map { |data, total| { data: data, total: total } }
  end

  def ocorrencias_por_especie(indicadores)
    indicadores
      .group(:nome_cientifico, :nome_popular)
      .sum(:quantidade)
      .map { |(nome_cientifico, nome_popular), total| { nome_cientifico: nome_cientifico, nome_popular: nome_popular, total: total } }
      .sort_by { |e| -e[:total] }
  end

  def pontos_mapa(indicadores)
    indicadores
      .pluck(:latitude, :longitude, :nome_cientifico, :quantidade)
      .map { |lat, lng, especie, qtd| { latitude: lat, longitude: lng, nome_cientifico: especie, quantidade: qtd } }
  end

  def registros_por_especie_tempo(indicadores)
    contagens = indicadores
      .group(:ano, :mes, :nome_cientifico, :is_endemica)
      .count

    contagens
      .map { |(ano, mes, nome_cientifico, is_endemica), total| { ano: ano, mes: mes, nome_cientifico: nome_cientifico, is_endemica: is_endemica, total: total } }
      .sort_by { |e| [ e[:ano], e[:mes], e[:nome_cientifico] ] }
  end

  def especies_distintas_por_mes(indicadores)
    indicadores
      .group(:ano, :mes)
      .distinct
      .count(:nome_cientifico)
      .sort_by { |chave, _| chave }
      .map { |(ano, mes), total| { ano: ano, mes: mes, total: total } }
  end
end
