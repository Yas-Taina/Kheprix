# frozen_string_literal: true

require "test_helper"

class PermissoesConvitesTest < PermissoesBase
  setup do
    @convidado = Usuario.create!(nome: "Convidado", email: "convidado_#{SecureRandom.hex(4)}@teste.com", password: "senha123")
    @convite = Convite.create!(
      estudo_id: @estudo.id,
      proprietario_envio_id: @proprietario.id,
      email_convidado: @convidado.email
    )
  end

  test "proprietario pode listar convites" do
    get "/estudos/#{@estudo.id}/convites", headers: auth(@token_prop)
    assert_response :ok
  end

  test "colaborador nao pode listar convites" do
    get "/estudos/#{@estudo.id}/convites", headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode listar convites" do
    get "/estudos/#{@estudo.id}/convites", headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode listar convites" do
    get "/estudos/#{@estudo.id}/convites"
    assert_response :unauthorized
  end

  test "proprietario pode criar convite" do
    novo_email = "novo_#{SecureRandom.hex(4)}@teste.com"
    post "/estudos/#{@estudo.id}/convites",
         params: { email_convidado: novo_email },
         headers: auth(@token_prop)
    assert_response :created
  end

  test "colaborador nao pode criar convite" do
    post "/estudos/#{@estudo.id}/convites",
         params: { email_convidado: "outro@teste.com" },
         headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode criar convite" do
    post "/estudos/#{@estudo.id}/convites",
         params: { email_convidado: "outro@teste.com" },
         headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode criar convite" do
    post "/estudos/#{@estudo.id}/convites", params: { email_convidado: "outro@teste.com" }
    assert_response :unauthorized
  end

  test "proprietario pode cancelar convite" do
    delete "/estudos/#{@estudo.id}/convites/#{@convite.id}",
           headers: auth(@token_prop)
    assert_response :no_content
  end

  test "colaborador nao pode cancelar convite" do
    delete "/estudos/#{@estudo.id}/convites/#{@convite.id}",
           headers: auth(@token_colab)
    assert_response :forbidden
  end

  test "forasteiro nao pode cancelar convite" do
    delete "/estudos/#{@estudo.id}/convites/#{@convite.id}",
           headers: auth(@token_forasteiro)
    assert_response :forbidden
  end

  test "sem token nao pode cancelar convite" do
    delete "/estudos/#{@estudo.id}/convites/#{@convite.id}"
    assert_response :unauthorized
  end
end
