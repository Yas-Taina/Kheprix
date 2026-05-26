# frozen_string_literal: true

require "test_helper"

class PermissoesEstudosTest < PermissoesBase
  VARIAVEIS_VALIDAS = [ { nome: "Var1", nivel_aplicacao: "campanha", tipo_dado: "string" } ].freeze

  test "colaborador autenticado pode criar estudo" do
    post "/estudos",
         params: { nome: "Novo Estudo", variaveis: VARIAVEIS_VALIDAS },
         headers: auth(@token_colab)
    assert_response :created
  end

  test "proprietario autenticado pode criar estudo" do
    post "/estudos",
         params: { nome: "Novo Estudo 2", variaveis: VARIAVEIS_VALIDAS },
         headers: auth(@token_prop)
    assert_response :created
  end

  test "sem token nao pode criar estudo" do
    post "/estudos", params: { nome: "Novo Estudo", variaveis: VARIAVEIS_VALIDAS }
    assert_response :unauthorized
  end

  test "proprietario unico deleta estudo e recebe 204" do
    delete "/estudos/#{@estudo.id}", headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador ao sair do estudo recebe 200 com mensagem" do
    delete "/estudos/#{@estudo.id}", headers: auth(@token_colab)
    assert_response :ok
    body = JSON.parse(response.body)
    assert_match(/descadastrado/i, body["mensagem"])
  end

  test "forasteiro nao encontra estudo alheio ao tentar deletar" do
    delete "/estudos/#{@estudo.id}", headers: auth(@token_forasteiro)
    assert_response :not_found
  end

  test "sem token nao pode deletar estudo" do
    delete "/estudos/#{@estudo.id}"
    assert_response :unauthorized
  end
end
