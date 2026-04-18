# frozen_string_literal: true

class AdicionaUltimoEstudoAcessadoEmUsuarios < ActiveRecord::Migration[8.0]
  def change
    add_reference :usuarios, :ultimo_estudo_acessado,
                  foreign_key: { to_table: :estudos, on_delete: :nullify },
                  null: true, index: true
  end
end
