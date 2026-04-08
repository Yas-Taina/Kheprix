# frozen_string_literal: true

class Dw::DimEspecie < DwRecord
  self.table_name = "dim_especie"
  self.primary_key = "id_especie"

  has_many :fatos, class_name: "Dw::FatoMedicaoEntomologica", foreign_key: "fk_especie"
end
