# frozen_string_literal: true

class Dw::DimEventoAmostragem < DwRecord
  self.table_name = "dim_evento_amostragem"
  self.primary_key = "id_evento"

  belongs_to :unidade_amostral, class_name: "Dw::DimUnidadeAmostral", foreign_key: "fk_unidade_amostral"
  has_many :fatos, class_name: "Dw::FatoMedicaoEntomologica", foreign_key: "fk_evento"
end
