# frozen_string_literal: true

class ExecutarAnaliseDto
  include ActiveModel::API

  attr_accessor :chave, :variavel_ids, :variavel_x_id, :variavel_y_id, :variavel_id,
                :filtro_data_inicio, :filtro_data_fim, :filtro_campanhas, :escopo_amostras,
                :filtro_area, :nivel_estudo, :agrupar_por, :grupo1_ids, :grupo2_ids,
                :nome_grupo1, :nome_grupo2

  validates :chave, presence: true

  def initialize(params = {})
    @chave = params[:chave]
    @variavel_ids = params[:variavel_ids]
    @variavel_x_id = params[:variavel_x_id]
    @variavel_y_id = params[:variavel_y_id]
    @variavel_id = params[:variavel_id]
    @filtro_data_inicio = params[:filtro_data_inicio]
    @filtro_data_fim = params[:filtro_data_fim]
    @filtro_campanhas = params[:filtro_campanhas]
    @escopo_amostras = params[:escopo_amostras]
    @filtro_area = params[:filtro_area]
    @nivel_estudo = params[:nivel_estudo]
    @agrupar_por = params[:agrupar_por]
    @grupo1_ids = params[:grupo1_ids]
    @grupo2_ids = params[:grupo2_ids]
    @nome_grupo1 = params[:nome_grupo1]
    @nome_grupo2 = params[:nome_grupo2]
  end

  def to_params
    {
      variavel_ids: variavel_ids,
      variavel_x_id: variavel_x_id,
      variavel_y_id: variavel_y_id,
      variavel_id: variavel_id,
      filtro_data_inicio: filtro_data_inicio,
      filtro_data_fim: filtro_data_fim,
      filtro_campanhas: filtro_campanhas,
      escopo_amostras: escopo_amostras,
      filtro_area: filtro_area,
      nivel_estudo: nivel_estudo,
      agrupar_por: agrupar_por,
      grupo1_ids: grupo1_ids,
      grupo2_ids: grupo2_ids,
      nome_grupo1: nome_grupo1,
      nome_grupo2: nome_grupo2
    }
  end
end
