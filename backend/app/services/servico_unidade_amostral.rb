# frozen_string_literal: true

class ServicoUnidadeAmostral
  def listar(campanha_id:)
    UnidadeAmostral.where(campanha_id: campanha_id).recentes
  end

  def buscar(id:)
    UnidadeAmostral.find(id)
  end

  def cadastrar(campanha:, nome:, latitude:, longitude:, raio:, metodo_coleta:, esforco_amostral:)
    UnidadeAmostral.create(
      campanha: campanha,
      nome: nome,
      latitude: latitude,
      longitude: longitude,
      raio: raio,
      metodo_coleta: metodo_coleta,
      esforco_amostral: esforco_amostral,
    )
  end

  def atualizar(unidade:, nome:, latitude:, longitude:, raio:, metodo_coleta:, esforco_amostral:)
    unidade.update(
      nome: nome,
      latitude: latitude,
      longitude: longitude,
      raio: raio,
      metodo_coleta: metodo_coleta,
      esforco_amostral: esforco_amostral,
    )
    unidade
  end

  def excluir(unidade:)
    unidade.eventos_amostragem.update_all(deleted_at: Time.zone.now)

    unidade.soft_delete
  end
end
