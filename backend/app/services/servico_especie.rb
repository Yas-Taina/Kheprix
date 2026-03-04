# frozen_string_literal: true

class ServicoEspecie
  # Listar todas as espécies ordenadas
  def listar
    Especie.ordenadas
  end

  # Buscar por ID — retorna nil se não encontrar
  def buscar_por_id(id)
    Especie.find_by(id: id)
  end

  # Criar uma nova espécie
  def criar(foto:, classe:, genero:, nome_popular:, nome_cientifico:, status_conservacao:, nativa_da_regiao:)
    Especie.create(
      foto: foto,
      classe: classe,
      genero: genero,
      nome_popular: nome_popular,
      nome_cientifico: nome_cientifico,
      status_conservacao: status_conservacao,
      nativa_da_regiao: nativa_da_regiao,
    )
  end

  # Atualizar uma espécie existente
  def atualizar(especie:, foto:, classe:, genero:, nome_popular:, nome_cientifico:, status_conservacao:, nativa_da_regiao:)
    especie.update(
      foto: foto,
      classe: classe,
      genero: genero,
      nome_popular: nome_popular,
      nome_cientifico: nome_cientifico,
      status_conservacao: status_conservacao,
      nativa_da_regiao: nativa_da_regiao,
    )
    especie
  end

  # Excluir uma espécie
  def destruir(especie)
    especie.destroy
  end
end