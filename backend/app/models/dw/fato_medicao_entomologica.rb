# frozen_string_literal: true

class Dw::FatoMedicaoEntomologica < DwRecord
  self.table_name = "fato_medicao_entomologica"
  self.primary_key = "id_registro"

  belongs_to :tempo, class_name: "Dw::DimTempo", foreign_key: "fk_data"
  belongs_to :especie, class_name: "Dw::DimEspecie", foreign_key: "fk_especie"
  belongs_to :estudo, class_name: "Dw::DimEstudo", foreign_key: "fk_estudo"
  belongs_to :evento, class_name: "Dw::DimEventoAmostragem", foreign_key: "fk_evento"

  has_many :variaveis_registro, class_name: "Dw::DimVariavelRegistro", foreign_key: "id_registro"

  scope :do_estudo, ->(id_estudo) { where(fk_estudo: id_estudo) }
  scope :da_especie, ->(id_especie) { where(fk_especie: id_especie) }
end
