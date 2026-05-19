# frozen_string_literal: true

class ExecutarAnaliseDto
  include ActiveModel::API

  FONTES = %w[variavel abundancia riqueza].freeze
  NIVEIS_AGREGACAO = %w[campanha unidade_amostral evento].freeze
  AGRUPAMENTOS = %w[campanha unidade_amostral evento mes ano estacao].freeze

  attr_accessor :chave, :variavel_ids, :variavel_x_id, :variavel_y_id, :variavel_id,
                :agrupar_por, :grupo1_ids, :grupo2_ids, :nome_grupo1, :nome_grupo2,
                :campanha_ids, :unidade_ids, :evento_ids, :data_inicio, :data_fim,
                :latitude_min, :latitude_max, :longitude_min, :longitude_max,
                :fonte, :fonte_x, :fonte_y, :nivel_agregacao

  validates :chave, presence: true
  validate :validar_datas
  validate :validar_bounding_box
  validate :validar_fontes
  validate :validar_nivel_agregacao
  validate :validar_agrupar_por

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
    @unidade_ids = params[:unidade_ids]
    @evento_ids = params[:evento_ids]
    @data_inicio, @erro_data_inicio = parse_data(params[:data_inicio])
    @data_fim, @erro_data_fim = parse_data(params[:data_fim])
    @latitude_min, @erro_lat_min = parse_decimal(params[:latitude_min], :latitude_min)
    @latitude_max, @erro_lat_max = parse_decimal(params[:latitude_max], :latitude_max)
    @longitude_min, @erro_lng_min = parse_decimal(params[:longitude_min], :longitude_min)
    @longitude_max, @erro_lng_max = parse_decimal(params[:longitude_max], :longitude_max)
    @fonte = params[:fonte]
    @fonte_x = params[:fonte_x]
    @fonte_y = params[:fonte_y]
    @nivel_agregacao = params[:nivel_agregacao]
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
      unidade_ids: unidade_ids,
      evento_ids: evento_ids,
      data_inicio: data_inicio,
      data_fim: data_fim,
      latitude_min: latitude_min,
      latitude_max: latitude_max,
      longitude_min: longitude_min,
      longitude_max: longitude_max,
      fonte: fonte,
      fonte_x: fonte_x,
      fonte_y: fonte_y,
      nivel_agregacao: nivel_agregacao
    }
  end

  private

  def parse_data(valor)
    return [ nil, nil ] if valor.blank?
    return [ valor, nil ] if valor.is_a?(Date)

    [ Date.parse(valor.to_s), nil ]
  rescue ArgumentError, TypeError
    [ nil, "formato de data inválido: #{valor}" ]
  end

  def parse_decimal(valor, campo)
    return [ nil, nil ] if valor.nil? || valor == ""
    return [ valor.to_d, nil ] if valor.is_a?(Numeric)

    [ BigDecimal(valor.to_s), nil ]
  rescue ArgumentError, TypeError
    [ nil, "#{campo} deve ser um número" ]
  end

  def validar_datas
    errors.add(:data_inicio, @erro_data_inicio) if @erro_data_inicio
    errors.add(:data_fim, @erro_data_fim) if @erro_data_fim

    if @data_inicio && @data_fim && @data_inicio > @data_fim
      errors.add(:data_fim, "deve ser maior ou igual a data_inicio")
    end
  end

  def validar_bounding_box
    errors.add(:latitude_min, @erro_lat_min) if @erro_lat_min
    errors.add(:latitude_max, @erro_lat_max) if @erro_lat_max
    errors.add(:longitude_min, @erro_lng_min) if @erro_lng_min
    errors.add(:longitude_max, @erro_lng_max) if @erro_lng_max

    # Faixa geografica: latitude em [-90, 90], longitude em [-180, 180]. Sem
    # range check aqui, BigDecimal aceita qualquer numero, a query de bounding
    # box devolve resultado pseudo-coerente (ex.: latitude_min=-200 nao filtra
    # nada porque toda UA real tem lat >= -200) e o usuario nunca ve "voce
    # digitou um numero impossivel".
    errors.add(:latitude_min, "deve estar entre -90 e 90")     if @latitude_min  && !@latitude_min.between?(-90, 90)
    errors.add(:latitude_max, "deve estar entre -90 e 90")     if @latitude_max  && !@latitude_max.between?(-90, 90)
    errors.add(:longitude_min, "deve estar entre -180 e 180")  if @longitude_min && !@longitude_min.between?(-180, 180)
    errors.add(:longitude_max, "deve estar entre -180 e 180")  if @longitude_max && !@longitude_max.between?(-180, 180)

    if @latitude_min && @latitude_max && @latitude_min > @latitude_max
      errors.add(:latitude_max, "deve ser maior ou igual a latitude_min")
    end

    if @longitude_min && @longitude_max && @longitude_min > @longitude_max
      errors.add(:longitude_max, "deve ser maior ou igual a longitude_min")
    end
  end

  def validar_fontes
    { fonte: @fonte, fonte_x: @fonte_x, fonte_y: @fonte_y }.each do |campo, valor|
      next if valor.nil? || valor == ""
      unless FONTES.include?(valor.to_s)
        errors.add(campo, "deve ser um de: #{FONTES.join(", ")}")
      end
    end
  end

  def validar_nivel_agregacao
    return if @nivel_agregacao.nil? || @nivel_agregacao == ""
    unless NIVEIS_AGREGACAO.include?(@nivel_agregacao.to_s)
      errors.add(:nivel_agregacao, "deve ser um de: #{NIVEIS_AGREGACAO.join(", ")}")
    end
  end

  def validar_agrupar_por
    return if @agrupar_por.nil? || @agrupar_por == ""
    unless AGRUPAMENTOS.include?(@agrupar_por.to_s)
      errors.add(:agrupar_por, "deve ser um de: #{AGRUPAMENTOS.join(", ")}")
    end
  end
end
