# frozen_string_literal: true

class Colaborador < ApplicationRecord
  self.table_name = "colaboradores"
  self.primary_key = [ :estudo_id, :usuario_id ]

  belongs_to :estudo
  belongs_to :usuario

  enum :perfil, { colaborador: 0, proprietario: 1 }
end
