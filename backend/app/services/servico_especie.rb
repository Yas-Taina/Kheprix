# frozen_string_literal: true

class ServicoEspecie
  include SalvaFotoBase64

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
    foto_base64 = atributos.delete(:foto)
    atributos[:foto] = salvar_foto_base64(foto_base64, estudo_id: estudo_id, tipo: "especies") if foto_base64.present?
    Especie.create(atributos.merge(estudo_id: estudo_id))
  end

  # Atualizar uma espécie existente
  def atualizar(especie:, atributos:)
    if atributos.key?(:foto)
      foto_base64 = atributos.delete(:foto)
      if foto_base64.present?
        remover_foto(especie.foto)
        atributos[:foto] = salvar_foto_base64(foto_base64, estudo_id: especie.estudo_id, tipo: "especies")
      else
        remover_foto(especie.foto)
        atributos[:foto] = nil
      end
    end
    especie.update(atributos)
    especie
  end

  # Excluir uma espécie
  def destruir(especie)
    especie.registro_ocorrencias.update_all(deleted_at: Time.zone.now)
    especie.soft_delete
  end
end
