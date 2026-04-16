# frozen_string_literal: true

class ExecutarAnaliseDto
  include ActiveModel::API

  attr_accessor :chave, :variavel_ids, :variavel_x_id, :variavel_y_id, :variavel_id,
                :agrupar_por, :grupo1_ids, :grupo2_ids, :nome_grupo1, :nome_grupo2,
                :campanha_ids

  validates :chave, presence: true

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
    }
  end
end
