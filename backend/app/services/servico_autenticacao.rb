# frozen_string_literal: true

class ServicoAutenticacao
  EXPIRACAO_EM_HORAS = 24

  def autenticar(email:, senha:)
    usuario = Usuario.find_by(email: email&.downcase&.strip)
    return unless usuario&.authenticate(senha)

    gerar_token(usuario)
  end

  def verificar_token(token:)
    payload = decodificar_token(token)
    return unless payload

    Usuario.find_by(id: payload["sub"])
  end

  def solicitar_redefinicao_senha(email:)
    usuario = Usuario.find_by(email: email&.downcase&.strip)
    return unless usuario

    token = usuario.signed_id(purpose: :redefinicao_senha, expires_in: 15.minutes)
    UsuarioMailer.redefinicao_senha(usuario: usuario, token: token).deliver_later
  end

  def validar_token_redefinicao(token:)
    Usuario.find_signed(token, purpose: :redefinicao_senha)
  end

  def redefinir_senha(token:, nova_senha:)
    usuario = validar_token_redefinicao(token: token)
    return unless usuario

    usuario.update(password: nova_senha)
    usuario
  end

  private

  def gerar_token(usuario)
    payload = {
      sub: usuario.id,
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
