# frozen_string_literal: true

require "test_helper"

class PermissoesCampanhasTest < PermissoesBase
  setup do
    @campanha = Campanha.create!(estudo_id: @estudo.id, nome: "Campanha Teste", data_inicio: "2025-01-01")
  end

  PARAMS_CRIAR = { nome: "Nova Campanha", data_inicio: "2025-06-01" }.freeze
  PARAMS_EDITAR = { nome: "Campanha Editada", data_inicio: "2025-07-01", descricao: nil }.freeze

  test "proprietario pode listar campanhas" do
    get "/estudos/#{@estudo.id}/campanhas", headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode listar campanhas" do
    get "/estudos/#{@estudo.id}/campanhas", headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode listar campanhas" do
    get "/estudos/#{@estudo.id}/campanhas", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar campanhas" do
    get "/estudos/#{@estudo.id}/campanhas"
    assert_response :unauthorized
  end

  test "proprietario pode criar campanha" do
    post "/estudos/#{@estudo.id}/campanhas",
         params: PARAMS_CRIAR,
         headers: auth(@token_prop)
    assert_response :created
  end

  test "colaborador pode criar campanha" do
    post "/estudos/#{@estudo.id}/campanhas",
         params: PARAMS_CRIAR,
         headers: auth(@token_colab)
    assert_response :created
  end

  test "forasteiro nao pode criar campanha" do
    post "/estudos/#{@estudo.id}/campanhas",
         params: PARAMS_CRIAR,
         headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar campanha" do
    post "/estudos/#{@estudo.id}/campanhas", params: PARAMS_CRIAR
    assert_response :unauthorized
  end

  test "proprietario pode atualizar campanha" do
    patch "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador pode atualizar campanha" do
    patch "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_colab)
    assert_response :ok
  end

  test "forasteiro nao pode atualizar campanha" do
    patch "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
          params: PARAMS_EDITAR,
          headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode atualizar campanha" do
    patch "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}", params: PARAMS_EDITAR
    assert_response :unauthorized
  end

  test "proprietario pode deletar campanha" do
    delete "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
           headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode deletar campanha" do
    delete "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
           headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode deletar campanha" do
    delete "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}",
           headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode deletar campanha" do
    delete "/estudos/#{@estudo.id}/campanhas/#{@campanha.id}"
    assert_response :unauthorized
  end
end
