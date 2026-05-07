# frozen_string_literal: true

module ValidaValoresVariaveis
  MAX_VALORES_VARIAVEIS = 200

  private

  def valida_valores_variaveis_criacao
    return if valores_variaveis.blank?
    return unless estrutura_valores_variaveis_valida?

    var_ids = valores_variaveis.filter_map { |vv| vv[:variavel_id] }
    if var_ids.length != var_ids.uniq.length
      errors.add(:valores_variaveis, "contém variavel_id duplicados")
    end

    valores_variaveis.each_with_index do |vv, indice|
      if vv[:id].present?
        errors.add(:base, "Valor variável #{indice + 1}: id não permitido em criação (use PATCH para atualizar)")
      end
      if vv[:variavel_id].blank?
        errors.add(:base, "Valor variável #{indice + 1}: variavel_id não pode ficar em branco")
      end
      if vv[:valor].nil? || vv[:valor] == ""
        errors.add(:base, "Valor variável #{indice + 1}: valor não pode ficar em branco")
      end
    end
  end

  def valida_valores_variaveis_edicao
    return if valores_variaveis.blank?
    return unless estrutura_valores_variaveis_valida?

    ids = valores_variaveis.filter_map { |vv| vv[:id] }
    if ids.length != ids.uniq.length
      errors.add(:valores_variaveis, "contém ids duplicados")
    end

    var_ids_novos = valores_variaveis.select { |vv| vv[:id].blank? }.filter_map { |vv| vv[:variavel_id] }
    if var_ids_novos.length != var_ids_novos.uniq.length
      errors.add(:valores_variaveis, "contém variavel_id duplicados em novos itens")
    end

    valores_variaveis.each_with_index do |vv, indice|
      if vv[:id].present?
        unless vv[:id].is_a?(Integer) && vv[:id] > 0
          errors.add(:base, "Valor variável #{indice + 1}: id deve ser inteiro positivo")
        end
      else
        if vv[:variavel_id].blank?
          errors.add(:base, "Valor variável #{indice + 1}: variavel_id é obrigatório para novos valores")
        end
      end
      if vv[:valor].nil? || vv[:valor] == ""
        errors.add(:base, "Valor variável #{indice + 1}: valor não pode ficar em branco")
      end
    end
  end

  def estrutura_valores_variaveis_valida?
    unless valores_variaveis.is_a?(Array)
      errors.add(:valores_variaveis, "deve ser um array")
      return false
    end
    if valores_variaveis.length > MAX_VALORES_VARIAVEIS
      errors.add(:valores_variaveis, "excede o limite de #{MAX_VALORES_VARIAVEIS} itens")
      return false
    end
    unless valores_variaveis.all? { |vv| vv.is_a?(Hash) || vv.is_a?(ActionController::Parameters) }
      errors.add(:valores_variaveis, "cada item deve ser um objeto")
      return false
    end
    true
  end
end
