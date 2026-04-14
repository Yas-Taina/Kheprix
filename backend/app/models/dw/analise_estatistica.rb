# frozen_string_literal: true

class Dw::AnaliseEstatistica < DwRecord
  self.table_name = "analises_estatisticas"
  self.primary_key = [ :id_registro, :id_variavel ]

  scope :do_estudo, ->(id_estudo) { where(id_estudo: id_estudo) }
end
