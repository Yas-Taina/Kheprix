# frozen_string_literal: true

class ServicoUnidadeAmostral
  include GerenciaValoresVariaveis

  def listar(campanha_id:)
    UnidadeAmostral.where(campanha_id: campanha_id).recentes
  end

  def buscar(id:)
    UnidadeAmostral.find(id)
  end

  def cadastrar(campanha:, nome:, latitude:, longitude:, raio:, metodo_coleta:, esforco_amostral:, valores_variaveis: nil)
    unidade = UnidadeAmostral.new(
      campanha: campanha,
      nome: nome,
      latitude: latitude,
      longitude: longitude,
      raio: raio,
      metodo_coleta: metodo_coleta,
      esforco_amostral: esforco_amostral,
    )
    validar_variaveis_compat(unidade, valores_variaveis, estudo_id: campanha.estudo_id, nivel: :unidade)
    return unidade if unidade.errors.any?

    ActiveRecord::Base.transaction do
      unidade.save!
      criar_valores_variaveis(unidade, valores_variaveis)
      unidade
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def atualizar(unidade:, nome:, latitude:, longitude:, raio:, metodo_coleta:, esforco_amostral:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      unidade.update!(
        nome: nome,
        latitude: latitude,
        longitude: longitude,
        raio: raio,
        metodo_coleta: metodo_coleta,
        esforco_amostral: esforco_amostral,
      )
      sincronizar_valores_variaveis(unidade, valores_variaveis)
      unidade
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  rescue ActiveRecord::RecordNotFound => e
    unidade.errors.add(:base, e.message)
    unidade
  end

  def excluir(unidade:)
    agora = Time.zone.now

    unidade.eventos_amostragem.each do |ea|
      ea.registro_ocorrencias.update_all(deleted_at: agora)
    end
    unidade.eventos_amostragem.update_all(deleted_at: agora)

    unidade.valores_variaveis.update_all(deleted_at: agora)

    unidade.soft_delete
  end

end
