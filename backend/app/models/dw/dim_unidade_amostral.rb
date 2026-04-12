# frozen_string_literal: true

class Dw::DimUnidadeAmostral < DwRecord
  self.table_name = "dim_unidade_amostral"
  self.primary_key = "id_unidade"

  belongs_to :campanha, class_name: "Dw::DimCampanha", foreign_key: "fk_campanha"
  has_many :eventos, class_name: "Dw::DimEventoAmostragem", foreign_key: "fk_unidade_amostral"
end
