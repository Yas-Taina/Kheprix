# frozen_string_literal: true

require "builder"

class GenericoHashParaXml
  def self.call(payload, raiz: "resultado")
    new.call(payload, raiz: raiz)
  end

  def call(payload, raiz:)
    xml = Builder::XmlMarkup.new(indent: 2)
    xml.instruct! :xml, version: "1.0", encoding: "UTF-8"
    escrever(xml, raiz, payload)
    xml.target!
  end

  private

  def escrever(xml, tag, valor)
    tag_sanitizada = sanitizar(tag)

    case valor
    when Hash
      xml.tag!(tag_sanitizada) do
        valor.each { |chave, item| escrever(xml, chave.to_s, item) }
      end
    when Array
      xml.tag!(tag_sanitizada) do
        valor.each { |item| escrever(xml, "item", item) }
      end
    when nil
      xml.tag!(tag_sanitizada, nil)
    else
      xml.tag!(tag_sanitizada, valor.to_s)
    end
  end

  def sanitizar(nome)
    ascii = I18n.transliterate(nome.to_s)
    slug = ascii.gsub(/[^A-Za-z0-9_]+/, "_").gsub(/_+/, "_").gsub(/^_|_$/, "")
    slug = "tag" if slug.empty?
    slug = "_#{slug}" if slug.match?(/^\d/)
    slug
  end
end
