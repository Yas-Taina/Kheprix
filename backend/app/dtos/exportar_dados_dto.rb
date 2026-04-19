# frozen_string_literal: true

class ExportarDadosDto
  include ActiveModel::API

  AGRUPAMENTOS = %w[registro_ocorrencia evento_amostragem unidade_amostral campanha especie].freeze
  FORMATOS = %w[csv xml].freeze

  attr_accessor :agrupamento, :formato

  validates :formato, presence: true, inclusion: { in: FORMATOS }
  validates :agrupamento, presence: true, inclusion: { in: AGRUPAMENTOS }, if: -> { formato == "csv" }

  def initialize(params = {})
    @agrupamento = params[:agrupamento]
    @formato = params[:formato]
  end
end
