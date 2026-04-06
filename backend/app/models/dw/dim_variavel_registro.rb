# frozen_string_literal: true

class Dw::DimVariavelRegistro < DwRecord
  self.table_name = "dim_variavel_registro"
  self.primary_key = [:id_registro, :id_variavel]

  belongs_to :fato, class_name: "Dw::FatoMedicaoEntomologica", foreign_key: "id_registro"
end
