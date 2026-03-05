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
      campanha.valores_variaveis.destroy_all
      criar_valores_variaveis(campanha, valores_variaveis)
      campanha
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def excluir(campanha:)
    campanha.destroy
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
