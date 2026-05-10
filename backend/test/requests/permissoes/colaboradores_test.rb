# frozen_string_literal: true

require "test_helper"

class PermissoesColaboradoresTest < PermissoesBase
  # ── INDEX ──────────────────────────────────────────────────────────────────
  test "proprietario pode listar colaboradores" do
    get "/estudos/#{@estudo.id}/colaboradores", headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador nao pode listar colaboradores" do
    get "/estudos/#{@estudo.id}/colaboradores", headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode listar colaboradores" do
    get "/estudos/#{@estudo.id}/colaboradores", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar colaboradores" do
    get "/estudos/#{@estudo.id}/colaboradores"
    assert_response :unauthorized
  end

  # ── UPDATE (alterar perfil) ─────────────────────────────────────────────────
  test "proprietario pode promover colaborador a proprietario" do
    patch "/estudos/#{@estudo.id}/colaboradores/#{@colaborador_user.id}",
          params: { perfil: "proprietario" },
          headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador nao pode alterar perfil de outro membro" do
    patch "/estudos/#{@estudo.id}/colaboradores/#{@proprietario.id}",
          params: { perfil: "colaborador" },
          headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "proprietario unico nao pode se rebaixar a colaborador" do
    patch "/estudos/#{@estudo.id}/colaboradores/#{@proprietario.id}",
          params: { perfil: "colaborador" },
          headers: auth(@token_prop)
    assert_response :unprocessable_entity
  end

  test "sem token nao pode alterar perfil" do
    patch "/estudos/#{@estudo.id}/colaboradores/#{@colaborador_user.id}",
          params: { perfil: "proprietario" }
    assert_response :unauthorized
  end

  # ── DESTROY (remover membro) ────────────────────────────────────────────────
  test "proprietario pode remover colaborador do estudo" do
    delete "/estudos/#{@estudo.id}/colaboradores/#{@colaborador_user.id}",
           headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode remover outro membro do estudo" do
    delete "/estudos/#{@estudo.id}/colaboradores/#{@proprietario.id}",
           headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "proprietario nao pode remover a si mesmo" do
    delete "/estudos/#{@estudo.id}/colaboradores/#{@proprietario.id}",
           headers: auth(@token_prop)
    assert_response :unprocessable_entity
  end

  test "forasteiro nao pode remover membro" do
    delete "/estudos/#{@estudo.id}/colaboradores/#{@colaborador_user.id}",
           headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode remover membro" do
    delete "/estudos/#{@estudo.id}/colaboradores/#{@colaborador_user.id}"
    assert_response :unauthorized
  end
end
