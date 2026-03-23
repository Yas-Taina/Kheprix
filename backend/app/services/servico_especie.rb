# frozen_string_literal: true

class ServicoEspecie
  # Listar espécies de um estudo, com filtros opcionais
  def listar(estudo_id:, filtros:)
    especies = Especie.do_estudo(estudo_id)
    especies = especies.por_nome_popular(filtros.nome_popular) if filtros.nome_popular.present?
especies.ordenadas
  end

  # Buscar por ID dentro de um estudo — retorna nil se não encontrar
  def buscar_por_id(estudo_id:, id:)
    Especie.do_estudo(estudo_id).find_by(id: id)
  end

  # Criar uma nova espécie
  def criar(estudo_id:, atributos:)
    Especie.create(atributos.merge(estudo_id: estudo_id))
  end

  # Atualizar uma espécie existente
  def atualizar(especie:, atributos:)
    especie.update(atributos)
    especie
  end

  # Excluir uma espécie
  def destruir(especie)
    especie.destroy
  end
end
