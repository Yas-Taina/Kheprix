# frozen_string_literal: true

class ExportarDadosDto
  include ActiveModel::API

  AGRUPAMENTOS = %w[registro_ocorrencia evento_amostragem unidade_amostral campanha especie].freeze
  FORMATOS = %w[csv].freeze

  attr_accessor :agrupamento, :formato

  validates :agrupamento, presence: true, inclusion: { in: AGRUPAMENTOS }
  validates :formato, inclusion: { in: FORMATOS }, allow_blank: true

  def initialize(params = {})
    @agrupamento = params[:agrupamento] || "registro_ocorrencia"
    @formato = params[:formato] || "csv"
  end
end
