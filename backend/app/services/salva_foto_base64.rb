# frozen_string_literal: true

module SalvaFotoBase64
  def salvar_foto_base64(base64_string, estudo_id:, tipo:)
    return nil if base64_string.blank?

    if base64_string.start_with?("data:")
      _header, encoded = base64_string.split(",", 2)
      extensao = _header.match(%r{image/(\w+)})&.captures&.first || "png"
    else
      encoded = base64_string
      extensao = "png"
    end

    dados = Base64.decode64(encoded)
    nome_arquivo = "#{SecureRandom.uuid}.#{extensao}"
    diretorio = Rails.root.join("storage", "fotos", "estudos", estudo_id.to_s, tipo)
    FileUtils.mkdir_p(diretorio)
    caminho = diretorio.join(nome_arquivo)
    File.binwrite(caminho, dados)

    "/fotos/estudos/#{estudo_id}/#{tipo}/#{nome_arquivo}"
  end

  def remover_foto(url_path)
    return if url_path.blank?

    caminho = Rails.root.join("storage", url_path.delete_prefix("/"))
    File.delete(caminho) if File.exist?(caminho)
  end
end
