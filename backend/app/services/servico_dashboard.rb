# frozen_string_literal: true

class ServicoDashboard
  def resumo_ultimo_estudo(usuario:)
    estudo = Estudo.por_usuario(usuario).order(updated_at: :desc).first
    return nil unless estudo

    registros = RegistroOcorrencia
      .joins(evento_amostragem: { unidade_amostral: :campanha })
      .where(campanhas: { estudo_id: estudo.id })

    especie_ids_observadas = registros.distinct.pluck(:especie_id)

    {
      id: estudo.id,
      nome: estudo.nome,
      updated_at: estudo.updated_at,
      data_inicio: estudo.campanhas.minimum(:data_inicio),
      total_registros: registros.count,
      total_especies: especie_ids_observadas.size,
      especies_ameacadas: Especie.where(id: especie_ids_observadas)
        .where.not(status_conservacao: [ nil, "" ]).count,
      especies_nativas: Especie.where(id: especie_ids_observadas, endemismo: true).count,
      especies_invasoras: Especie.where(id: especie_ids_observadas, endemismo: false).count,
      total_individuos: registros.sum(:qtde_individuos)
    }
  end
end
