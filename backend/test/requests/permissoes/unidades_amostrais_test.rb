# frozen_string_literal: true

require "test_helper"

class PermissoesUnidadesAmostraisTest < PermissoesBase
  setup do
    @campanha = Campanha.create!(estudo_id: @estudo.id, nome: "Campanha UA", data_inicio: "2025-01-01")
    @unidade  = UnidadeAmostral.create!(
      campanha_id: @campanha.id,
      nome: "UA Teste",
      latitude: -15.0,
      longitude: -47.0
    )
  end

  def base_url
    "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}/unidades_amostrais"
  end

  PARAMS_CRIAR = { nome: "UA Nova", latitude: -16.0, longitude: -48.0 }.freeze
  PARAMS_EDITAR = { nome: "UA Editada", latitude: -16.5, longitude: -48.5 }.freeze

  test "proprietario pode listar unidades amostrais" do
    get base_url, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode listar unidades amostrais" do
    get base_url, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode listar unidades amostrais" do
    get base_url, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar unidades amostrais" do
    get base_url
    assert_response :unauthorized
  end

  test "proprietario pode criar unidade amostral" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_prop)
    assert_response :created
  end

  test "colaborador pode criar unidade amostral" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_colab)
    assert_response :created
  end

  test "forasteiro nao pode criar unidade amostral" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar unidade amostral" do
    post base_url, params: PARAMS_CRIAR
    assert_response :unauthorized
  end

  test "proprietario pode atualizar unidade amostral" do
    patch "#{base_url}/#{@unidade.id}", params: PARAMS_EDITAR, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode atualizar unidade amostral" do
    patch "#{base_url}/#{@unidade.id}", params: PARAMS_EDITAR, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode atualizar unidade amostral" do
    patch "#{base_url}/#{@unidade.id}", params: PARAMS_EDITAR, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode atualizar unidade amostral" do
    patch "#{base_url}/#{@unidade.id}", params: PARAMS_EDITAR
    assert_response :unauthorized
  end

  test "proprietario pode deletar unidade amostral" do
    delete "#{base_url}/#{@unidade.id}", headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode deletar unidade amostral" do
    delete "#{base_url}/#{@unidade.id}", headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode deletar unidade amostral" do
    delete "#{base_url}/#{@unidade.id}", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode deletar unidade amostral" do
    delete "#{base_url}/#{@unidade.id}"
    assert_response :unauthorized
  end
end
