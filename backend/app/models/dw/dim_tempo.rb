# frozen_string_literal: true

class Dw::DimTempo < DwRecord
  self.table_name = "dim_tempo"
  self.primary_key = "id_data"

  has_many :fatos, class_name: "Dw::FatoMedicaoEntomologica", foreign_key: "fk_data"

  scope :por_ano, ->(ano) { where(ano: ano) }
  scope :por_estacao, ->(estacao) { where(estacao: estacao) }
  scope :entre_datas, ->(inicio, fim) { where(data_completa: inicio..fim) }
end
