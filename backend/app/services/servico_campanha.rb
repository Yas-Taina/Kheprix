# frozen_string_literal: true

class ServicoCampanha
  include GerenciaValoresVariaveis

  def listar(estudo_id:)
    Campanha.where(estudo_id: estudo_id).order(updated_at: :desc)
  end

  def buscar(id:)
    Campanha.find(id)
  end

  def cadastrar(estudo:, nome:, data_inicio:, descricao:, valores_variaveis: nil)
    campanha = Campanha.new(
      estudo: estudo,
      nome: nome,
      data_inicio: data_inicio,
      descricao: descricao,
    )
    validar_variaveis_compat(campanha, valores_variaveis, estudo_id: estudo.id, nivel: :campanha)
    return campanha if campanha.errors.any?

    ActiveRecord::Base.transaction do
      campanha.save!
      criar_valores_variaveis(campanha, valores_variaveis)
      campanha
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def atualizar(campanha:, nome:, data_inicio:, descricao:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      campanha.update!(
        nome: nome,
        data_inicio: data_inicio,
        descricao: descricao,
      )
      sincronizar_valores_variaveis(campanha, valores_variaveis)
      campanha
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  rescue ActiveRecord::RecordNotFound => e
    campanha.errors.add(:base, e.message)
    campanha
  end

  def excluir(campanha:)
    agora = Time.zone.now

    campanha.unidades_amostrais.each do |ua|
      ua.eventos_amostragem.each do |ea|
        ea.registro_ocorrencias.update_all(deleted_at: agora)
      end
      ua.eventos_amostragem.update_all(deleted_at: agora)
    end
    campanha.unidades_amostrais.update_all(deleted_at: agora)

    campanha.valores_variaveis.update_all(deleted_at: agora)

    campanha.soft_delete
  end

end
