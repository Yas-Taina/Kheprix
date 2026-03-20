# frozen_string_literal: true

class ServicoEventoAmostragem
  def listar(unidade_amostral_id:)
    EventoAmostragem.where(unidade_amostral_id: unidade_amostral_id).ordenados
  end

  def buscar_por_id(id:)
    EventoAmostragem.find_by(id: id)
  end

  def criar(unidade_amostral:, horario_inicio:, horario_fim:, esforco_real:)
    EventoAmostragem.create(
      unidade_amostral: unidade_amostral,
      horario_inicio: horario_inicio,
      horario_fim: horario_fim,
      esforco_real: esforco_real,
    )
  end

  def atualizar(evento:, horario_inicio:, horario_fim:, esforco_real:)
    evento.update(
      horario_inicio: horario_inicio,
      horario_fim: horario_fim,
      esforco_real: esforco_real,
    )
    evento
  end

  def excluir(evento:)
    evento.destroy
  end
end
