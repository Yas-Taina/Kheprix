# frozen_string_literal: true

class ServicoRegistroOcorrencia
  include SalvaFotoBase64

  def listar(evento_id:)
    RegistroOcorrencia.do_evento(evento_id).por_data
  end

  def buscar_por_id(evento_id:, id:)
    RegistroOcorrencia.do_evento(evento_id).find_by(id: id)
  end

  def criar(evento_id:, estudo_id:, atributos:)
    foto_base64 = atributos.delete(:foto)
    atributos[:foto] = salvar_foto_base64(foto_base64, estudo_id: estudo_id, tipo: "registro_ocorrencias") if foto_base64.present?
    RegistroOcorrencia.create(atributos.merge(evento_amostragem_id: evento_id))
  end

  def atualizar(registro:, estudo_id:, atributos:)
    if atributos.key?(:foto)
      foto_base64 = atributos.delete(:foto)
      if foto_base64.present?
        remover_foto(registro.foto)
        atributos[:foto] = salvar_foto_base64(foto_base64, estudo_id: estudo_id, tipo: "registro_ocorrencias")
      else
        remover_foto(registro.foto)
        atributos[:foto] = nil
      end
    end
    registro.update(atributos)
    registro
  end

  def destruir(registro)
    registro.soft_delete
  end
end
