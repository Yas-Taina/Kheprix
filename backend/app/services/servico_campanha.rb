# frozen_string_literal: true

class ServicoCampanha
  def listar(estudo_id:)
    Campanha.where(estudo_id: estudo_id).order(updated_at: :desc)
  end

  def buscar(id:)
    Campanha.find(id)
  end

  def cadastrar(estudo:, nome:, data_inicio:, data_fim:, descricao:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      campanha = Campanha.create!(
        estudo: estudo,
        nome: nome,
        data_inicio: data_inicio,
        data_fim: data_fim,
        descricao: descricao,
      )
      criar_valores_variaveis(campanha, valores_variaveis)
      campanha
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def atualizar(campanha:, nome:, data_inicio:, data_fim:, descricao:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      campanha.update!(
        nome: nome,
        data_inicio: data_inicio,
        data_fim: data_fim,
        descricao: descricao,
      )
      unless valores_variaveis.nil?
        campanha.valores_variaveis.destroy_all
        criar_valores_variaveis(campanha, valores_variaveis)
      end
      campanha
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def excluir(campanha:)
    agora = Time.zone.now

    campanha.unidades_amostrais.each do |ua|
      ua.eventos_amostragem.update_all(deleted_at: agora)
    end
    campanha.unidades_amostrais.update_all(deleted_at: agora)

    campanha.valores_variaveis.update_all(deleted_at: agora)

    campanha.soft_delete
  end

  private

  def criar_valores_variaveis(campanha, valores_variaveis)
    return if valores_variaveis.blank?

    valores_variaveis.each do |vv|
      ValorVariavel.create!(
        variavel_id: vv[:variavel_id],
        id_nivel_aplicacao: campanha.id,
        valor: vv[:valor],
      )
    end
  end
end
