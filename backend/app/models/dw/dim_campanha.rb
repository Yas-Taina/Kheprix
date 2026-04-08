# frozen_string_literal: true

class Dw::DimCampanha < DwRecord
  self.table_name = "dim_campanha"
  self.primary_key = "id_campanha"

  belongs_to :estudo, class_name: "Dw::DimEstudo", foreign_key: "fk_estudo"
  has_many :unidades_amostrais, class_name: "Dw::DimUnidadeAmostral", foreign_key: "fk_campanha"
end
