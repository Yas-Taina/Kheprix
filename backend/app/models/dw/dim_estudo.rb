# frozen_string_literal: true

class Dw::DimEstudo < DwRecord
  self.table_name = "dim_estudo"
  self.primary_key = "id_estudo"

  has_many :campanhas, class_name: "Dw::DimCampanha", foreign_key: "fk_estudo"
  has_many :fatos, class_name: "Dw::FatoMedicaoEntomologica", foreign_key: "fk_estudo"
end
