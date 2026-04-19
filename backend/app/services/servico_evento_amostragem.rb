# frozen_string_literal: true

class ServicoEventoAmostragem
  include GerenciaValoresVariaveis

  def listar(unidade_amostral_id:)
    EventoAmostragem.where(unidade_amostral_id: unidade_amostral_id).ordenados
  end

  def buscar_por_id(id:)
    EventoAmostragem.find_by(id: id)
  end

  def criar(unidade_amostral:, horario_inicio:, horario_fim:, esforco_real:, valores_variaveis: nil)
    evento = EventoAmostragem.new(
      unidade_amostral: unidade_amostral,
      horario_inicio: horario_inicio,
      horario_fim: horario_fim,
      esforco_real: esforco_real,
    )
    validar_variaveis_compat(evento, valores_variaveis, estudo_id: unidade_amostral.campanha.estudo_id, nivel: :evento)
    return evento if evento.errors.any?

    ActiveRecord::Base.transaction do
      evento.save!
      criar_valores_variaveis(evento, valores_variaveis)
      evento
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def atualizar(evento:, horario_inicio:, horario_fim:, esforco_real:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      evento.update!(
        horario_inicio: horario_inicio,
        horario_fim: horario_fim,
        esforco_real: esforco_real,
      )
      sincronizar_valores_variaveis(evento, valores_variaveis)
      evento
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  rescue ActiveRecord::RecordNotFound => e
    evento.errors.add(:base, e.message)
    evento
  end

  def excluir(evento:)
    agora = Time.zone.now
    evento.registro_ocorrencias.update_all(deleted_at: agora)
    evento.valores_variaveis.update_all(deleted_at: agora)
    evento.soft_delete
  end

end
