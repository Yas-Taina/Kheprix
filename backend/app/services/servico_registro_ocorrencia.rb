# frozen_string_literal: true

class ServicoRegistroOcorrencia
  def listar(evento_id:)
    RegistroOcorrencia.do_evento(evento_id).por_data
  end

  def buscar_por_id(evento_id:, id:)
    RegistroOcorrencia.do_evento(evento_id).find_by(id: id)
  end

  def criar(evento_id:, atributos:)
    RegistroOcorrencia.create(atributos.merge(evento_amostragem_id: evento_id))
  end

  def atualizar(registro:, atributos:)
    registro.update(atributos)
    registro
  end

  def destruir(registro)
    registro.soft_delete
  end
end
