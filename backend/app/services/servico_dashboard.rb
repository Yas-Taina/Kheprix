# frozen_string_literal: true

class ServicoDashboard
  def resumo_ultimo_estudo(usuario:)
    # OLTP: busca o estudo mais recente do usuário
    estudo = Estudo.por_usuario(usuario).order(updated_at: :desc).first
    return nil unless estudo

    # DW: métricas da camada de apresentação
    indicadores = Dw::IndicadoresDashboard.do_estudo(estudo.id)

    {
      id: estudo.id,
      nome: estudo.nome,
      updated_at: estudo.updated_at,
      data_inicio: indicadores.minimum(:data_inicio_campanha),
      total_registros: indicadores.count,
      total_especies: indicadores.distinct.count(:nome_cientifico),
      especies_ameacadas: indicadores.where(is_ameacada: true).distinct.count(:nome_cientifico),
      especies_nativas: indicadores.where(is_endemica: true).distinct.count(:nome_cientifico),
      especies_invasoras: indicadores.where(is_endemica: false).distinct.count(:nome_cientifico),
      total_individuos: indicadores.sum(:quantidade)
    }
  end
end
