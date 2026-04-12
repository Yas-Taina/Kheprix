# frozen_string_literal: true

class ServicoDashboard
  def resumo_ultimo_estudo(usuario:)
    estudo_oltp = Estudo.por_usuario(usuario).order(updated_at: :desc).first
    return nil unless estudo_oltp

    estudo_dw = Dw::DimEstudo.find_by(id_estudo: estudo_oltp.id)
    return nil unless estudo_dw

    fatos = Dw::FatoMedicaoEntomologica.do_estudo(estudo_dw.id_estudo)
    especie_ids = fatos.distinct.pluck(:fk_especie)
    especies = Dw::DimEspecie.where(id_especie: especie_ids)

    {
      id: estudo_oltp.id,
      nome: estudo_dw.nome_estudo,
      updated_at: estudo_oltp.updated_at,
      data_inicio: estudo_dw.campanhas.minimum(:data_inicio),
      total_registros: fatos.count,
      total_especies: especie_ids.size,
      especies_ameacadas: especies.where.not(status_conservacao: [ nil, "" ]).count,
      especies_nativas: especies.where(endemismo: true).count,
      especies_invasoras: especies.where(endemismo: false).count,
      total_individuos: fatos.sum(:quantidade)
    }
  end
end
