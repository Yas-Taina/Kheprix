# frozen_string_literal: true

class Dw::FatoVariaveisUnificadas < DwRecord
  self.table_name = "fato_variaveis_unificadas"
  self.primary_key = [:id_registro, :id_variavel]
end
