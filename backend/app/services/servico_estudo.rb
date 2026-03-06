# frozen_string_literal: true

class ServicoEstudo
  def pesquisar(usuario:, filtros:)
    estudos = Estudo.por_usuario(usuario)
    estudos = estudos.por_nome(filtros.nome) if filtros.nome.present?
    estudos = estudos.criado_a_partir_de(filtros.criado_a_partir_de) if filtros.criado_a_partir_de.present?
    estudos = estudos.criado_ate(filtros.criado_ate) if filtros.criado_ate.present?
    estudos = estudos.atualizado_a_partir_de(filtros.atualizado_a_partir_de) if filtros.atualizado_a_partir_de.present?
    estudos = estudos.atualizado_ate(filtros.atualizado_ate) if filtros.atualizado_ate.present?

    estudos = estudos
      .select("estudos.*, colaboradores.perfil AS perfil_colaborador")
      .order(updated_at: :desc)

    estudos.map do |estudo|
      estudo.as_json.merge("perfil" => Colaborador.perfis.key(estudo.perfil_colaborador))
    end
  end

  def deletar(id:, usuario:)
    estudo = Estudo.find_by(id: id)
    return :nao_encontrado unless estudo

    colaborador = Colaborador.find_by(estudo_id: estudo.id, usuario_id: usuario.id)
    return :nao_encontrado unless colaborador
    return :nao_autorizado unless colaborador.proprietario?

    if estudo.colaboradores.where(perfil: :proprietario).count > 1
      colaborador.destroy!
      :descadastrado
    else
      estudo.destroy!
      :ok
    end
  end

  def cadastrar(nome:, observacoes:, usuario:, variaveis:)
    ActiveRecord::Base.transaction do
      estudo = Estudo.create!(nome: nome, observacoes: observacoes)
      Colaborador.create!(estudo: estudo, usuario: usuario, perfil: :proprietario)
      ServicoVariavel.new.criar_em_lote(estudo: estudo, variaveis_params: variaveis)
      estudo
    end
  rescue ActiveRecord::RecordInvalid => e
    e.record
  end
end
