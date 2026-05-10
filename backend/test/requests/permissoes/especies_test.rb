# frozen_string_literal: true

require "test_helper"

class PermissoesEspeciesTest < PermissoesBase
  setup do
    @especie = Especie.create!(
      estudo_id: @estudo.id,
      classe: "Aves", ordem: "Passeriformes",
      familia: "Thraupidae", genero: "Tangara", especie: "chilensis",
      endemismo: false
    )
  end

  PARAMS_CRIAR = {
    classe: "Mammalia", ordem: "Primates",
    familia: "Hominidae", genero: "Homo", especie: "sapiens",
    endemismo: false
  }.freeze

  PARAMS_EDITAR = { classe: "Reptilia" }.freeze

  # ── INDEX ──────────────────────────────────────────────────────────────────
  test "proprietario pode listar especies" do
    get "/estudos/#{@estudo.id}/especies", headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode listar especies" do
    get "/estudos/#{@estudo.id}/especies", headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode listar especies" do
    get "/estudos/#{@estudo.id}/especies", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar especies" do
    get "/estudos/#{@estudo.id}/especies"
    assert_response :unauthorized
  end

  # ── CREATE ─────────────────────────────────────────────────────────────────
  # Envia como JSON para que `endemismo: false` chegue como booleano (não string "false")
  test "proprietario pode criar especie" do
    post "/estudos/#{@estudo.id}/especies",
         params: PARAMS_CRIAR,
         headers: auth(@token_prop),
         as: :json
    assert_response :created
  end

  test "colaborador pode criar especie" do
    post "/estudos/#{@estudo.id}/especies",
         params: PARAMS_CRIAR,
         headers: auth(@token_colab),
         as: :json
    assert_response :created
  end

  test "forasteiro nao pode criar especie" do
    post "/estudos/#{@estudo.id}/especies",
         params: PARAMS_CRIAR,
         headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar especie" do
    post "/estudos/#{@estudo.id}/especies", params: PARAMS_CRIAR
    assert_response :unauthorized
  end

  # ── UPDATE ─────────────────────────────────────────────────────────────────
  test "proprietario pode atualizar especie" do
    patch "/estudos/#{@estudo.id}/especies/#{@especie.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode atualizar especie" do
    patch "/estudos/#{@estudo.id}/especies/#{@especie.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode atualizar especie" do
    patch "/estudos/#{@estudo.id}/especies/#{@especie.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode atualizar especie" do
    patch "/estudos/#{@estudo.id}/especies/#{@especie.id}", params: PARAMS_EDITAR
    assert_response :unauthorized
  end

  # ── DELETE ─────────────────────────────────────────────────────────────────
  test "proprietario pode deletar especie" do
    delete "/estudos/#{@estudo.id}/especies/#{@especie.id}",
           headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode deletar especie" do
    delete "/estudos/#{@estudo.id}/especies/#{@especie.id}",
           headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode deletar especie" do
    delete "/estudos/#{@estudo.id}/especies/#{@especie.id}",
           headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode deletar especie" do
    delete "/estudos/#{@estudo.id}/especies/#{@especie.id}"
    assert_response :unauthorized
  end
end
