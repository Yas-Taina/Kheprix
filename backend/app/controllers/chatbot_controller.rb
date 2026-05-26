# frozen_string_literal: true

require "net/http"

class ChatbotController < ApplicationController
  before_action :autenticar_requisicao!

  TIMEOUT_S = 60
  private_constant :TIMEOUT_S

  def query
    pergunta = params[:pergunta].to_s.strip
    if pergunta.blank?
      render json: { erro: "O campo 'pergunta' é obrigatório." }, status: :unprocessable_entity
      return
    end

    estudo_ids = filtrar_estudo_ids(params[:estudo_ids])
    if estudo_ids.empty?
      render json: { erro: "Nenhum estudo válido fornecido." }, status: :unprocessable_entity
      return
    end

    payload = { pergunta: pergunta, estudo_ids: estudo_ids, usuario_id: usuario_atual.id }
    encaminhar_ao_chatbot("/query", payload)
  end

  def insights
    estudo_ids = filtrar_estudo_ids(params[:estudo_ids])
    if estudo_ids.empty?
      render json: { erro: "Nenhum estudo válido fornecido." }, status: :unprocessable_entity
      return
    end

    payload = { estudo_ids: estudo_ids, usuario_id: usuario_atual.id }
    encaminhar_ao_chatbot("/insights", payload)
  end

  private

  def filtrar_estudo_ids(ids_requisitados)
    return [] if ids_requisitados.blank?

    ids_usuario = Estudo.por_usuario(usuario_atual).pluck(:id)
    Array(ids_requisitados).map(&:to_i) & ids_usuario
  end

  def encaminhar_ao_chatbot(caminho, payload)
    uri = URI("#{chatbot_url}#{caminho}")

    http = Net::HTTP.new(uri.host, uri.port)
    http.open_timeout = TIMEOUT_S
    http.read_timeout = TIMEOUT_S

    request = Net::HTTP::Post.new(uri)
    request["Content-Type"]  = "application/json"
    request["X-Internal-Key"] = chatbot_internal_key
    request.body = payload.to_json

    response = http.request(request)
    render json: JSON.parse(response.body), status: response.code.to_i
  rescue Net::OpenTimeout, Net::ReadTimeout
    render json: { erro: "O serviço de IA não respondeu a tempo. Tente novamente." },
           status: :gateway_timeout
  rescue StandardError => e
    Rails.logger.error("chatbot_proxy_erro caminho=#{caminho} erro=#{e.message}")
    render json: { erro: "Erro ao comunicar com o serviço de IA." }, status: :bad_gateway
  end

  def chatbot_url
    ENV.fetch("CHATBOT_URL", "http://chatbot:8000")
  end

  def chatbot_internal_key
    ENV.fetch("CHATBOT_INTERNAL_KEY")
  end
end
