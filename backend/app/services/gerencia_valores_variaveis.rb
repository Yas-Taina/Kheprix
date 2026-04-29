# frozen_string_literal: true

module GerenciaValoresVariaveis
  def validar_variaveis_compat(entidade, vv_list, estudo_id:, nivel:)
    return if vv_list.blank?

    ids = vv_list.filter_map { |vv| vv[:variavel_id] }.uniq
    mapa = Variavel.where(id: ids).index_by(&:id)

    vv_list.each_with_index do |vv, idx|
      v = mapa[vv[:variavel_id]]
      if v.nil? || v.estudo_id != estudo_id
        entidade.errors.add(:base, "Valor variável #{idx + 1}: variavel_id #{vv[:variavel_id]} não pertence a este estudo")
      elsif v.nivel_aplicacao.to_s != nivel.to_s
        entidade.errors.add(:base, "Valor variável #{idx + 1}: variavel_id #{vv[:variavel_id]} é de nível '#{v.nivel_aplicacao}', esperado '#{nivel}'")
      elsif !valor_compativel?(vv[:valor], v.tipo_dado)
        entidade.errors.add(:base, "Valor variável #{idx + 1}: valor '#{vv[:valor]}' incompatível com tipo_dado '#{v.tipo_dado}'")
      end
    end
  end

  def valor_compativel?(valor, tipo_dado)
    return false if valor.nil? || valor == ""

    case tipo_dado.to_s
    when "number"
      begin
        JSON.parse(valor.to_s).is_a?(Numeric)
      rescue JSON::ParserError
        false
      end
    when "date"
      begin
        Date.iso8601(valor.to_s)
        true
      rescue Date::Error, TypeError
        false
      end
    when "boolean"
      [ true, false, "true", "false" ].include?(valor)
    else
      true
    end
  end

  def criar_valores_variaveis(entidade, valores_variaveis)
    return if valores_variaveis.blank?

    valores_variaveis.each do |vv|
      ValorVariavel.create!(
        variavel_id: vv[:variavel_id],
        id_nivel_aplicacao: entidade.id,
        valor: vv[:valor].to_s,
      )
    end
  end

  def sincronizar_valores_variaveis(entidade, payload)
    return if payload.nil?

    agora = Time.zone.now
    ids_payload = payload.map { |vv| vv[:id].to_i }
    ids_existentes = entidade.valores_variaveis.pluck(:id)
    alterado = false

    ids_remover = ids_existentes - ids_payload
    if ids_remover.any?
      ValorVariavel.where(id: ids_remover)
        .update_all(deleted_at: agora, updated_at: agora)
      alterado = true
    end

    payload.each do |vv|
      existente = entidade.valores_variaveis.find_by(id: vv[:id])
      raise ActiveRecord::RecordNotFound, "valor_variavel #{vv[:id]} não pertence a este recurso" if existente.nil?

      unless valor_compativel?(vv[:valor], existente.variavel.tipo_dado)
        entidade.errors.add(:base, "valor_variavel #{vv[:id]}: valor '#{vv[:valor]}' incompatível com tipo_dado '#{existente.variavel.tipo_dado}'")
        raise ActiveRecord::RecordInvalid, entidade
      end

      novo_valor = vv[:valor].to_s
      if existente.valor.to_s != novo_valor
        existente.update!(valor: novo_valor)
        alterado = true
      end
    end

    entidade.touch if alterado
    entidade.valores_variaveis.reload
  end
end
