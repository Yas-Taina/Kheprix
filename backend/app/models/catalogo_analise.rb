# frozen_string_literal: true

class CatalogoAnalise
  ANALISES = [
    { chave: "lognormal", nome: "Log-Normal", categoria: "modelo_distribuicao", endpoint_r: nil, endpoint_r_grafico: "/analise/lognormal", tipo_dado: "abundancias" },
    { chave: "logserie", nome: "Log-Série", categoria: "modelo_distribuicao", endpoint_r: nil, endpoint_r_grafico: "/analise/logserie", tipo_dado: "abundancias" },
    { chave: "geometrica", nome: "Geométrica", categoria: "modelo_distribuicao", endpoint_r: nil, endpoint_r_grafico: "/analise/geometrica", tipo_dado: "abundancias" },
    { chave: "vara_quebrada", nome: "Vara Quebrada", categoria: "modelo_distribuicao", endpoint_r: nil, endpoint_r_grafico: "/analise/vara_quebrada", tipo_dado: "abundancias" },
    { chave: "rarefacao", nome: "Rarefação", categoria: "rarefacao", endpoint_r: "/analise/rarefacao", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "jackknife1", nome: "Jackknife 1ª Ordem", categoria: "estimador_riqueza", endpoint_r: "/analise/jackknife1", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "jackknife2", nome: "Jackknife 2ª Ordem", categoria: "estimador_riqueza", endpoint_r: "/analise/jackknife2", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "chao1", nome: "Chao1", categoria: "estimador_riqueza", endpoint_r: "/analise/chao1", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "chao2", nome: "Chao2", categoria: "estimador_riqueza", endpoint_r: "/analise/chao2", endpoint_r_grafico: nil, tipo_dado: "abundancias_por_amostra" },
    { chave: "bootstrap", nome: "Bootstrap", categoria: "estimador_riqueza", endpoint_r: "/analise/bootstrap", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "ace", nome: "ACE", categoria: "estimador_riqueza", endpoint_r: "/analise/ace", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "ice", nome: "ICE", categoria: "estimador_riqueza", endpoint_r: "/analise/ice", endpoint_r_grafico: nil, tipo_dado: "abundancias_por_amostra" },
    { chave: "shannon", nome: "Shannon-Wiener", categoria: "indice_diversidade", endpoint_r: "/analise/shannon", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "simpson", nome: "Simpson", categoria: "indice_diversidade", endpoint_r: "/analise/simpson", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "margalef", nome: "Margalef", categoria: "indice_diversidade", endpoint_r: "/analise/margalef", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "pielou", nome: "Pielou", categoria: "indice_diversidade", endpoint_r: "/analise/pielou", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "berger_parker", nome: "Berger-Parker", categoria: "indice_diversidade", endpoint_r: "/analise/berger_parker", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "brillouin", nome: "Brillouin", categoria: "indice_diversidade", endpoint_r: "/analise/brillouin", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "macintosh", nome: "McIntosh", categoria: "indice_diversidade", endpoint_r: "/analise/macintosh", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "hurlbert", nome: "Hurlbert", categoria: "indice_diversidade", endpoint_r: "/analise/hurlbert", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "mcnaughton", nome: "McNaughton", categoria: "indice_diversidade", endpoint_r: "/analise/mcnaughton", endpoint_r_grafico: nil, tipo_dado: "abundancias" },
    { chave: "teste_t", nome: "Teste T", categoria: "teste_hipotese", endpoint_r: "/analise/teste_t", endpoint_r_grafico: nil, tipo_dado: "dois_grupos" },
    { chave: "ks", nome: "Kolmogorov-Smirnov", categoria: "teste_hipotese", endpoint_r: "/analise/ks", endpoint_r_grafico: nil, tipo_dado: "dois_grupos" },
    { chave: "shapiro", nome: "Shapiro-Wilk", categoria: "teste_hipotese", endpoint_r: "/analise/shapiro", endpoint_r_grafico: nil, tipo_dado: "vetor_unico" },
    { chave: "anova", nome: "ANOVA", categoria: "teste_hipotese", endpoint_r: "/analise/anova", endpoint_r_grafico: nil, tipo_dado: "multiplos_grupos" },
    { chave: "kruskal", nome: "Kruskal-Wallis", categoria: "teste_hipotese", endpoint_r: "/analise/kruskal", endpoint_r_grafico: nil, tipo_dado: "multiplos_grupos" },
    { chave: "pearson", nome: "Correlação de Pearson", categoria: "correlacao", endpoint_r: "/analise/pearson", endpoint_r_grafico: "/analise/pearson_grafico", tipo_dado: "dois_vetores" },
    { chave: "spearman", nome: "Correlação de Spearman", categoria: "correlacao", endpoint_r: "/analise/spearman", endpoint_r_grafico: "/analise/spearman_grafico", tipo_dado: "dois_vetores" },
    { chave: "kendall", nome: "Correlação de Kendall", categoria: "correlacao", endpoint_r: "/analise/kendall", endpoint_r_grafico: "/analise/kendall_grafico", tipo_dado: "dois_vetores" },
    { chave: "regressao_linear", nome: "Regressão Linear", categoria: "regressao", endpoint_r: nil, endpoint_r_grafico: "/analise/regressao_linear", tipo_dado: "dois_vetores" },
    { chave: "jaccard", nome: "Índice de Jaccard", categoria: "similaridade", endpoint_r: "/analise/jaccard", endpoint_r_grafico: "/analise/jaccard_grafico", tipo_dado: "abundancias_por_amostra" },
    { chave: "bray_curtis", nome: "Bray-Curtis", categoria: "similaridade", endpoint_r: "/analise/bray_curtis", endpoint_r_grafico: "/analise/bray_curtis_grafico", tipo_dado: "abundancias_por_amostra" },
    { chave: "morisita", nome: "Morisita", categoria: "similaridade", endpoint_r: "/analise/morisita", endpoint_r_grafico: "/analise/morisita_grafico", tipo_dado: "abundancias_por_amostra" },
    { chave: "sorensen", nome: "Sørensen", categoria: "similaridade", endpoint_r: "/analise/sorensen", endpoint_r_grafico: "/analise/sorensen_grafico", tipo_dado: "abundancias_por_amostra" },
    { chave: "rda", nome: "RDA", categoria: "multivariada", endpoint_r: nil, endpoint_r_grafico: "/analise/rda", tipo_dado: "abundancias_com_variaveis" },
    { chave: "cca", nome: "CCA", categoria: "multivariada", endpoint_r: nil, endpoint_r_grafico: "/analise/cca", tipo_dado: "abundancias_com_variaveis" },
    { chave: "nmds", nome: "nMDS", categoria: "multivariada", endpoint_r: nil, endpoint_r_grafico: "/analise/nmds", tipo_dado: "abundancias_por_amostra" },
    { chave: "pca", nome: "PCA", categoria: "multivariada", endpoint_r: nil, endpoint_r_grafico: "/analise/pca", tipo_dado: "abundancias_por_amostra" },
    { chave: "modelo_gaussiano", nome: "GLM Gaussiano", categoria: "glm", endpoint_r: "/analise/modelo_gaussiano", endpoint_r_grafico: "/analise/modelo_gaussiano_grafico", tipo_dado: "dois_vetores" },
    { chave: "modelo_gamma", nome: "GLM Gamma", categoria: "glm", endpoint_r: "/analise/modelo_gamma", endpoint_r_grafico: "/analise/modelo_gamma_grafico", tipo_dado: "dois_vetores" },
    { chave: "modelo_poisson", nome: "GLM Poisson", categoria: "glm", endpoint_r: "/analise/modelo_poisson", endpoint_r_grafico: "/analise/modelo_poisson_grafico", tipo_dado: "dois_vetores" },
    { chave: "modelo_binomial_negativa", nome: "GLM Binomial Negativa", categoria: "glm", endpoint_r: "/analise/modelo_binomial_negativa", endpoint_r_grafico: "/analise/modelo_binomial_grafico", tipo_dado: "dois_vetores" },
    { chave: "michaelis_menten", nome: "Michaelis-Menten", categoria: "acumulacao", endpoint_r: nil, endpoint_r_grafico: "/analise/michaelis_menten", tipo_dado: "matriz_acumulacao" }
  ].freeze

  def self.buscar(chave)
    ANALISES.find { |a| a[:chave] == chave }
  end

  def self.listar
    ANALISES
  end

  def self.listar_por_categoria
    ANALISES.group_by { |a| a[:categoria] }
  end
end
