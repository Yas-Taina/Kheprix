# frozen_string_literal: true

class ServicoColaborador
  def listar(estudo_id:)
    Colaborador.where(estudo_id: estudo_id).includes(:usuario)
  end

  def alterar_perfil(colaborador:, perfil:)
    colaborador.update(perfil: perfil)
    colaborador
  end

  def remover(colaborador:)
    colaborador.destroy
  end
end
