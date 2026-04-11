# frozen_string_literal: true

class ServicoUnidadeAmostral
  def listar(campanha_id:)
    UnidadeAmostral.where(campanha_id: campanha_id).recentes
  end

  def buscar(id:)
    UnidadeAmostral.find(id)
  end

  def cadastrar(campanha:, nome:, latitude:, longitude:, raio:, metodo_coleta:, esforco_amostral:, valores_variaveis: nil)
    ActiveRecord::Base.transaction do
      unidade = UnidadeAmostral.create!(
        campanha: campanha,
        nome: nome,
        latitude: latitude,
        longitude: longitude,
        raio: raio,
        metodo_coleta: metodo_coleta,
        esforco_amostral: esforco_amostral,
      )
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
      unless valores_variaveis.nil?
        unidade.valores_variaveis.destroy_all
        criar_valores_variaveis(unidade, valores_variaveis)
      end
      unidade
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
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

  private

  def criar_valores_variaveis(unidade, valores_variaveis)
    return if valores_variaveis.blank?

    valores_variaveis.each do |vv|
      ValorVariavel.create!(
        variavel_id: vv[:variavel_id],
        id_nivel_aplicacao: unidade.id,
        valor: vv[:valor],
      )
    end
  end
end
