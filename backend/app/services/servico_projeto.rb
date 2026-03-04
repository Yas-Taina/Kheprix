# frozen_string_literal: true

class ServicoProjeto
  # Listar todos os projetos ativos
  def listar
    Projeto.ativos.recentes
  end

  # Buscar por ID — retorna nil se não encontrar
  def buscar_por_id(id)
    Projeto.find_by(id: id)
  end

  # Criar um novo projeto
  def criar(nome:, descricao:)
    Projeto.create(nome: nome, descricao: descricao)
  end

  # Atualizar um projeto existente
  def atualizar(projeto:, nome:, descricao:, ativo:)
    projeto.update(nome: nome, descricao: descricao, ativo: ativo)
    projeto
  end

  # Excluir um projeto
  def destruir(projeto)
    projeto.destroy
  end
end