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

  HUMAN_ATTR = {
    "chave" => "Análise",
    "data_inicio" => "Data inicial",
    "data_fim" => "Data final",
    "latitude_min" => "Latitude mínima",
    "latitude_max" => "Latitude máxima",
    "longitude_min" => "Longitude mínima",
    "longitude_max" => "Longitude máxima",
    "fonte" => "Fonte dos dados",
    "fonte_x" => "Fonte do eixo X",
    "fonte_y" => "Fonte do eixo Y",
    "nivel_agregacao" => "Nível de agregação",
    "agrupar_por" => "Agrupamento"
  }.freeze

  def self.human_attribute_name(attr, options = {})
    HUMAN_ATTR[attr.to_s] || super
  end

  validates :chave, presence: true
  validate :validar_datas
  validate :validar_bounding_box
  validate :validar_fontes
  validate :validar_nivel_agregacao
  validate :validar_agrupar_por
  validate :validar_parametros_por_chave

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

  def validar_parametros_por_chave
    return if @chave.blank?

    analise = CatalogoAnalise.buscar(@chave.to_s)
    return unless analise

    case analise[:tipo_dado]
    when "abundancias_com_variaveis"
      if @variavel_ids.blank? || !@variavel_ids.is_a?(Array) || @variavel_ids.empty?
        errors.add(:base, "Selecione pelo menos uma variável ambiental para #{analise[:nome]}.")
      end
    when "dois_vetores"
      fonte_x = (@fonte_x || "variavel").to_s
      fonte_y = (@fonte_y || "variavel").to_s
      if fonte_x == "variavel" && @variavel_x_id.blank?
        errors.add(:base, "Selecione a variável do eixo X para #{analise[:nome]}.")
      end
      if fonte_y == "variavel" && @variavel_y_id.blank?
        errors.add(:base, "Selecione a variável do eixo Y para #{analise[:nome]}.")
      end
    when "dois_grupos"
      if @grupo1_ids.blank? || !@grupo1_ids.is_a?(Array) || @grupo1_ids.empty?
        errors.add(:base, "Selecione pelo menos uma unidade amostral no Grupo 1 para #{analise[:nome]}.")
      end
      if @grupo2_ids.blank? || !@grupo2_ids.is_a?(Array) || @grupo2_ids.empty?
        errors.add(:base, "Selecione pelo menos uma unidade amostral no Grupo 2 para #{analise[:nome]}.")
      end
      if (@fonte || "variavel").to_s == "variavel" && @variavel_id.blank?
        errors.add(:base, "Selecione a variável a ser comparada para #{analise[:nome]}.")
      end
    when "multiplos_grupos"
      if @agrupar_por.blank?
        errors.add(:base, "Escolha como agrupar os dados (por campanha, mês, etc.) para #{analise[:nome]}.")
      end
      if (@fonte || "variavel").to_s == "variavel" && @variavel_id.blank?
        errors.add(:base, "Selecione a variável a ser comparada para #{analise[:nome]}.")
      end
    when "vetor_unico"
      if (@fonte || "variavel").to_s == "variavel" && @variavel_id.blank?
        errors.add(:base, "Selecione a variável a ser analisada para #{analise[:nome]}.")
      end
    end
  end
end
