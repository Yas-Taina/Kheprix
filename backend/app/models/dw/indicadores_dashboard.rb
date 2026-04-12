# frozen_string_literal: true

class Dw::IndicadoresDashboard < DwRecord
  self.table_name = "indicadores_dashboard"
  self.primary_key = "id_registro"

  belongs_to :estudo, class_name: "Dw::DimEstudo", foreign_key: "fk_estudo"

  scope :do_estudo, ->(id_estudo) { where(fk_estudo: id_estudo) }
end
