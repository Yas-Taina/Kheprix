# frozen_string_literal: true

class ExecutarAnaliseDto
  include ActiveModel::API

  attr_accessor :chave, :variavel_ids, :variavel_x_id, :variavel_y_id, :variavel_id,
                :agrupar_por, :grupo1_ids, :grupo2_ids, :nome_grupo1, :nome_grupo2,
                :campanha_ids, :data_inicio, :data_fim

  validates :chave, presence: true
  validate :validar_datas

  def initialize(params = {})
    @chave = params[:chave]
    @variavel_ids = params[:variavel_ids]
    @variavel_x_id = params[:variavel_x_id]
    @variavel_y_id = params[:variavel_y_id]
    @variavel_id = params[:variavel_id]
    @agrupar_por = params[:agrupar_por]
    @grupo1_ids = params[:grupo1_ids]
    @grupo2_ids = params[:grupo2_ids]
    @nome_grupo1 = params[:nome_grupo1]
    @nome_grupo2 = params[:nome_grupo2]
    @campanha_ids = params[:campanha_ids]
    @data_inicio, @erro_data_inicio = parse_data(params[:data_inicio])
    @data_fim, @erro_data_fim = parse_data(params[:data_fim])
  end

  def to_params
    {
      variavel_ids: variavel_ids,
      variavel_x_id: variavel_x_id,
      variavel_y_id: variavel_y_id,
      variavel_id: variavel_id,
      agrupar_por: agrupar_por,
      grupo1_ids: grupo1_ids,
      grupo2_ids: grupo2_ids,
      nome_grupo1: nome_grupo1,
      nome_grupo2: nome_grupo2,
      campanha_ids: campanha_ids,
      data_inicio: data_inicio,
      data_fim: data_fim,
    }
  end

  private

  def parse_data(valor)
    return [nil, nil] if valor.blank?
    return [valor, nil] if valor.is_a?(Date)

    [Date.parse(valor.to_s), nil]
  rescue ArgumentError, TypeError
    [nil, "formato de data inválido: #{valor}"]
  end

  def validar_datas
    errors.add(:data_inicio, @erro_data_inicio) if @erro_data_inicio
    errors.add(:data_fim, @erro_data_fim) if @erro_data_fim

    if @data_inicio && @data_fim && @data_inicio > @data_fim
      errors.add(:data_fim, "deve ser maior ou igual a data_inicio")
    end
  end
end
