# frozen_string_literal: true

require "zip"

module SalvaResultadoAnalise
  module_function

  def salvar(payload:, xml:, estudo_id:, chave:, nome:)
    diretorio = Rails.root.join("storage", "analises", "estudos", estudo_id.to_s, chave.to_s)
    FileUtils.mkdir_p(diretorio)

    nome_base = montar_nome_base(nome)
    nome_arquivo = resolver_colisao(diretorio, nome_base)
    caminho = diretorio.join(nome_arquivo)

    json = JSON.pretty_generate(payload)

    Zip::File.open(caminho.to_s, create: true) do |zip|
      zip.get_output_stream("resultado.json") { |io| io.write(json) }
      zip.get_output_stream("resultado.xml") { |io| io.write(xml) }
      if payload[:grafico].present?
        zip.get_output_stream("resultado.html") { |io| io.write(payload[:grafico]) }
      end
    end

    "/analises/estudos/#{estudo_id}/#{chave}/#{nome_arquivo}"
  end

  def montar_nome_base(nome)
    slug = slug(nome)
    timestamp = Time.zone.now.strftime("%d-%m-%Y_%H-%M")
    "analise_#{slug}_#{timestamp}"
  end

  def slug(texto)
    ascii = I18n.transliterate(texto.to_s)
    ascii.gsub(/[^A-Za-z0-9]+/, "_").gsub(/_+/, "_").gsub(/^_|_$/, "")
  end

  def resolver_colisao(diretorio, nome_base)
    candidato = "#{nome_base}.zip"
    return candidato unless File.exist?(diretorio.join(candidato))

    contador = 1
    loop do
      candidato = "#{nome_base}(#{contador}).zip"
      return candidato unless File.exist?(diretorio.join(candidato))

      contador += 1
    end
  end
end
