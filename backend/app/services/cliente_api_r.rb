# frozen_string_literal: true

require "net/http"
require "uri"
require "json"

class ClienteApiR
  BASE_URL = ENV.fetch("API_R_URL", "http://localhost:8000")

  STATUS_OK = :ok
  STATUS_ERRO_R = :erro_r
  STATUS_INDISPONIVEL = :indisponivel

  def executar(endpoint:, dados:)
    uri = URI("#{BASE_URL}#{endpoint}")
    requisicao = Net::HTTP::Post.new(uri, "Content-Type" => "application/json")
    requisicao.body = dados.to_json

    resposta = Net::HTTP.start(uri.hostname, uri.port, read_timeout: 60) do |http|
      http.request(requisicao)
    end

    if resposta.is_a?(Net::HTTPSuccess)
      conteudo, tipo = parsear_corpo(resposta)
      { status: STATUS_OK, tipo: tipo, conteudo: conteudo }
    else
      mensagem = extrair_mensagem_de_erro(resposta)
      Rails.logger.warn(
        "[ClienteApiR] erro_r endpoint=#{endpoint} http=#{resposta.code} msg=#{mensagem.inspect}",
      )
      { status: STATUS_ERRO_R, mensagem_erro: mensagem, http_status: resposta.code }
    end
  rescue Net::ReadTimeout, Net::OpenTimeout => e
    Rails.logger.warn("[ClienteApiR] timeout endpoint=#{endpoint} class=#{e.class}")
    { status: STATUS_INDISPONIVEL, mensagem_erro: "Timeout ao chamar a API R (#{e.class})" }
  rescue Errno::ECONNREFUSED, Errno::EHOSTUNREACH, Errno::ENETUNREACH, SocketError => e
    Rails.logger.warn("[ClienteApiR] indisponivel endpoint=#{endpoint} class=#{e.class} msg=#{e.message}")
    { status: STATUS_INDISPONIVEL, mensagem_erro: "API R inalcançável: #{e.class}" }
  rescue JSON::ParserError => e
    Rails.logger.warn("[ClienteApiR] json_invalido endpoint=#{endpoint} msg=#{e.message}")
    { status: STATUS_ERRO_R, mensagem_erro: "Resposta da API R não é JSON válido" }
  end

  private

  def parsear_corpo(resposta)
    if resposta.content_type&.include?("text/html")
      [ resposta.body, "html" ]
    else
      [ JSON.parse(resposta.body), "json" ]
    end
  end

  def extrair_mensagem_de_erro(resposta)
    return "" if resposta.body.to_s.empty?

    parsed = JSON.parse(resposta.body)
    parsed.is_a?(Hash) ? (parsed["error"] || parsed["mensagem"] || resposta.body) : resposta.body
  rescue JSON::ParserError
    resposta.body.to_s[0, 500]
  end
end
