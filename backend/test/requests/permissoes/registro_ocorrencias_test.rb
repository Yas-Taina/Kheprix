# frozen_string_literal: true

require "test_helper"

class PermissoesRegistroOcorrenciasTest < PermissoesBase
  setup do
    @campanha = Campanha.create!(estudo_id: @estudo.id, nome: "Campanha RO", data_inicio: "2025-01-01")
    @unidade  = UnidadeAmostral.create!(campanha_id: @campanha.id, nome: "UA RO", latitude: -15.0, longitude: -47.0)
    @evento   = EventoAmostragem.create!(
      unidade_amostral_id: @unidade.id,
      horario_inicio: "2025-01-01T08:00:00",
      esforco_real: "4 horas"
    )
    @especie  = Especie.create!(
      estudo_id: @estudo.id,
      classe: "Aves", ordem: "Passeriformes",
      familia: "Thraupidae", genero: "Tangara", especie: "chilensis",
      endemismo: false
    )
    @registro = RegistroOcorrencia.create!(
      evento_amostragem_id: @evento.id,
      especie_id: @especie.id,
      data: "2025-01-01",
      hora: "08:30:00",
      latitude: -15.0,
      longitude: -47.0
    )
  end

  def base_url
    "/estudos/#{@estudo.id}" \
      "/campanhas/#{@campanha.id}" \
      "/unidades_amostrais/#{@unidade.id}" \
      "/eventos_amostragem/#{@evento.id}" \
      "/registro_ocorrencias"
  end

  def params_criar
    {
      especie_id: @especie.id,
      data: "2025-02-01",
      hora: "09:00:00",
      latitude: -15.5,
      longitude: -47.5
    }
  end

  def params_editar
    {
      especie_id: @especie.id,
      data: "2025-02-02",
      hora: "10:00:00",
      latitude: -15.6,
      longitude: -47.6
    }
  end

  test "proprietario pode listar registros de ocorrencia" do
    get base_url, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode listar registros de ocorrencia" do
    get base_url, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode listar registros de ocorrencia" do
    get base_url, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar registros de ocorrencia" do
    get base_url
    assert_response :unauthorized
  end

  test "proprietario pode criar registro de ocorrencia" do
    post base_url, params: params_criar, headers: auth(@token_prop)
    assert_response :created
  end

  test "colaborador pode criar registro de ocorrencia" do
    post base_url, params: params_criar, headers: auth(@token_colab)
    assert_response :created
  end

  test "forasteiro nao pode criar registro de ocorrencia" do
    post base_url, params: params_criar, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar registro de ocorrencia" do
    post base_url, params: params_criar
    assert_response :unauthorized
  end

  test "proprietario pode atualizar registro de ocorrencia" do
    patch "#{base_url}/#{@registro.id}", params: params_editar, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode atualizar registro de ocorrencia" do
    patch "#{base_url}/#{@registro.id}", params: params_editar, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode atualizar registro de ocorrencia" do
    patch "#{base_url}/#{@registro.id}", params: params_editar, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode atualizar registro de ocorrencia" do
    patch "#{base_url}/#{@registro.id}", params: params_editar
    assert_response :unauthorized
  end

  test "proprietario pode deletar registro de ocorrencia" do
    delete "#{base_url}/#{@registro.id}", headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador tambem pode deletar registro de ocorrencia (lacuna de permissao)" do
    delete "#{base_url}/#{@registro.id}", headers: auth(@token_colab)
    assert_response :no_content
  end

  test "forasteiro nao pode deletar registro de ocorrencia" do
    delete "#{base_url}/#{@registro.id}", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode deletar registro de ocorrencia" do
    delete "#{base_url}/#{@registro.id}"
    assert_response :unauthorized
  end
end
