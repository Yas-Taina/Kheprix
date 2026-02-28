# frozen_string_literal: true

class ServicoAutenticacao
  EXPIRACAO_EM_HORAS = 24

  def autenticar(email:, senha:)
    usuario = ServicoUsuario.new.buscar_por_email(email)
    return unless usuario

    senha_armazenada = BCrypt::Password.new(usuario[:senha_hash])
    return unless senha_armazenada == senha

    gerar_token(usuario)
  end

  def verificar_token(token:)
    payload = decodificar_token(token)
    return unless payload

    usuario = ServicoUsuario.new.buscar_por_id(payload["sub"])
    return unless usuario

    usuario.except(:senha_hash)
  end

  private

  def gerar_token(usuario)
    payload = {
      sub: usuario[:id],
      exp: EXPIRACAO_EM_HORAS.hours.from_now.to_i,
      iat: Time.zone.now.to_i
    }
    JWT.encode(payload, segredo_jwt, "HS256")
  end

  def decodificar_token(token)
    JWT.decode(token, segredo_jwt, true, algorithm: "HS256").first
  rescue JWT::DecodeError
    nil
  end

  def segredo_jwt
    Rails.application.credentials.jwt_secret_key || Rails.application.secret_key_base
  end
end
