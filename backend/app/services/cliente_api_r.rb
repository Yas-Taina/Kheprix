# frozen_string_literal: true

require "net/http"
require "uri"
require "json"

class ClienteApiR
  BASE_URL = ENV.fetch("API_R_URL", "http://localhost:8000")

  def executar(endpoint:, dados:)
    uri = URI("#{BASE_URL}#{endpoint}")
    requisicao = Net::HTTP::Post.new(uri, "Content-Type" => "application/json")
    requisicao.body = dados.to_json

    resposta = Net::HTTP.start(uri.hostname, uri.port, read_timeout: 60) do |http|
      http.request(requisicao)
    end

    return nil unless resposta.is_a?(Net::HTTPSuccess)

    if resposta.content_type&.include?("text/html")
      { tipo: "html", conteudo: resposta.body }
    else
      { tipo: "json", conteudo: JSON.parse(resposta.body) }
    end
  rescue StandardError
    nil
  end
end
