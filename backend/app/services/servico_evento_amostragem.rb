# frozen_string_literal: true

class ServicoEventoAmostragem
  def listar(unidade_amostral_id:)
    EventoAmostragem.where(unidade_amostral_id: unidade_amostral_id).ordenados
  end

  def buscar_por_id(id:)
    EventoAmostragem.find_by(id: id)
  end

  def criar(unidade_amostral:, horario_inicio:, horario_fim:, esforco_real:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      evento = EventoAmostragem.create!(
        unidade_amostral: unidade_amostral,
        horario_inicio: horario_inicio,
        horario_fim: horario_fim,
        esforco_real: esforco_real,
      )
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
      unless valores_variaveis.nil?
        evento.valores_variaveis.destroy_all
        criar_valores_variaveis(evento, valores_variaveis)
      end
      evento
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def excluir(evento:)
    agora = Time.zone.now
    evento.registro_ocorrencias.update_all(deleted_at: agora)
    evento.valores_variaveis.update_all(deleted_at: agora)
    evento.soft_delete
  end

  private

  def criar_valores_variaveis(evento, valores_variaveis)
    return if valores_variaveis.blank?

    valores_variaveis.each do |vv|
      ValorVariavel.create!(
        variavel_id: vv[:variavel_id],
        id_nivel_aplicacao: evento.id,
        valor: vv[:valor],
      )
    end
  end
end
