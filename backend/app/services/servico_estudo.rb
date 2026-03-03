# frozen_string_literal: true

class ServicoEstudo
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
