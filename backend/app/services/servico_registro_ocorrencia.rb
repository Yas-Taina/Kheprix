# frozen_string_literal: true

class ServicoRegistroOcorrencia
  include SalvaFotoBase64
  include GerenciaValoresVariaveis

  def listar(evento_id:)
    RegistroOcorrencia.do_evento(evento_id).por_data
  end

  def buscar_por_id(evento_id:, id:)
    RegistroOcorrencia.do_evento(evento_id).find_by(id: id)
  end

  def criar(evento_id:, estudo_id:, atributos:, valores_variaveis: nil)
    registro_placeholder = RegistroOcorrencia.new(atributos.merge(evento_amostragem_id: evento_id))
    validar_variaveis_compat(registro_placeholder, valores_variaveis, estudo_id: estudo_id, nivel: :registro)
    return registro_placeholder if registro_placeholder.errors.any?

    foto_base64 = atributos.delete(:foto)
    atributos[:foto] = salvar_foto_base64(foto_base64, estudo_id: estudo_id, tipo: "registro_ocorrencias") if foto_base64.present?

    ActiveRecord::Base.transaction do
      registro = RegistroOcorrencia.create!(atributos.merge(evento_amostragem_id: evento_id))
      criar_valores_variaveis(registro, valores_variaveis)
      registro
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end

  def atualizar(registro:, estudo_id:, atributos:, valores_variaveis: nil)
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

    ActiveRecord::Base.transaction do
      registro.update!(atributos)
      sincronizar_valores_variaveis(registro, valores_variaveis)
      registro
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  rescue ActiveRecord::RecordNotFound => e
    registro.errors.add(:base, e.message)
    registro
  end

  def destruir(registro)
    registro.valores_variaveis.update_all(deleted_at: Time.zone.now)
    registro.soft_delete
  end

end
