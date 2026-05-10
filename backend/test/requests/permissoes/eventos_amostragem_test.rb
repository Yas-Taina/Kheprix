# frozen_string_literal: true

require "test_helper"

class PermissoesEventosAmostragemTest < PermissoesBase
  setup do
    @campanha = Campanha.create!(estudo_id: @estudo.id, nome: "Campanha EA", data_inicio: "2025-01-01")
    @unidade  = UnidadeAmostral.create!(campanha_id: @campanha.id, nome: "UA EA", latitude: -15.0, longitude: -47.0)
    @evento   = EventoAmostragem.create!(
      unidade_amostral_id: @unidade.id,
      horario_inicio: "2025-01-01T08:00:00",
      esforco_real: "4 horas"
    )
  end

  def base_url
    "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}/unidades_amostrais/#{@unidade.id}/eventos_amostragem"
  end

  PARAMS_CRIAR  = { horario_inicio: "2025-03-01T10:00:00", esforco_real: "2 horas" }.freeze
  PARAMS_EDITAR = { horario_inicio: "2025-03-01T12:00:00", esforco_real: "3 horas" }.freeze

  # ── INDEX ──────────────────────────────────────────────────────────────────
  test "proprietario pode listar eventos de amostragem" do
    get base_url, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode listar eventos de amostragem" do
    get base_url, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode listar eventos de amostragem" do
    get base_url, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar eventos de amostragem" do
    get base_url
    assert_response :unauthorized
  end

  # ── CREATE ─────────────────────────────────────────────────────────────────
  test "proprietario pode criar evento de amostragem" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_prop)
    assert_response :created
  end

  test "colaborador pode criar evento de amostragem" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_colab)
    assert_response :created
  end

  test "forasteiro nao pode criar evento de amostragem" do
    post base_url, params: PARAMS_CRIAR, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar evento de amostragem" do
    post base_url, params: PARAMS_CRIAR
    assert_response :unauthorized
  end

  # ── UPDATE ─────────────────────────────────────────────────────────────────
  test "proprietario pode atualizar evento de amostragem" do
    patch "#{base_url}/#{@evento.id}", params: PARAMS_EDITAR, headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode atualizar evento de amostragem" do
    patch "#{base_url}/#{@evento.id}", params: PARAMS_EDITAR, headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode atualizar evento de amostragem" do
    patch "#{base_url}/#{@evento.id}", params: PARAMS_EDITAR, headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode atualizar evento de amostragem" do
    patch "#{base_url}/#{@evento.id}", params: PARAMS_EDITAR
    assert_response :unauthorized
  end

  # ── DELETE ─────────────────────────────────────────────────────────────────
  test "proprietario pode deletar evento de amostragem" do
    delete "#{base_url}/#{@evento.id}", headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode deletar evento de amostragem" do
    delete "#{base_url}/#{@evento.id}", headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode deletar evento de amostragem" do
    delete "#{base_url}/#{@evento.id}", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode deletar evento de amostragem" do
    delete "#{base_url}/#{@evento.id}"
    assert_response :unauthorized
  end
end
