# Script de Teste para API de Análises do Sistema Kheprix
library(httr)
library(jsonlite)

base_url <- "http://localhost:8000"

# Função auxiliar para testar endpoints
testar_endpoint <- function(endpoint, dados, nome_teste) {
  cat("\n====================================\n")
  cat("Testando:", nome_teste, "\n")
  cat("====================================\n")
  
  tryCatch({
    resposta <- POST(
      url = paste0(base_url, endpoint),
      body = toJSON(dados, auto_unbox = TRUE),
      content_type_json()
    )
    
    if (status_code(resposta) == 200) {
      cat("✓ SUCESSO - Status:", status_code(resposta), "\n")
      
      content_type <- headers(resposta)$`content-type`
      
      if (grepl("application/json", content_type)) {
        resultado <- content(resposta, as = "parsed")
        print(resultado)
      } else if (grepl("text/html", content_type)) {
        cat("Gráfico HTML gerado com sucesso!\n")
        cat("Primeiros 200 caracteres:\n")
        html_content <- content(resposta, as = "text", encoding = "UTF-8")
        cat(substr(html_content, 1, 200), "...\n")
      }
    } else {
      cat("ERRO - Status:", status_code(resposta), "\n")
      print(content(resposta))
    }
  }, error = function(e) {
    cat("✗ ERRO DE CONEXÃO:", e$message, "\n")
  })
}

# Dados de teste

nomes_especies_exemplo <- c(
  "Apis mellifera",
  "Bombus terrestris", 
  "Xylocopa violacea",
  "Megachile rotundata",
  "Osmia bicornis",
  "Lasioglossum malachurum",
  "Andrena fulva",
  "Halictus rubicundus",
  "Anthophora plumipes",
  "Eucera nigrescens",
  "Colletes daviesanus"
)

abundancias_teste <- list(
  abundancias = c(45, 23, 12, 8, 5, 3, 2, 2, 1, 1, 1),
  nomes_especies = nomes_especies_exemplo
)

matriz_pa_teste <- list(
  matriz = matrix(c(
    1, 0, 1, 0, 1,
    1, 1, 0, 0, 1,
    0, 1, 1, 1, 0,
    1, 0, 1, 1, 1
  ), nrow = 4, byrow = TRUE),
  nomes_especies = c("Apis mellifera", "Bombus terrestris", "Xylocopa violacea", 
                     "Megachile rotundata", "Osmia bicornis"),
  nomes_amostras = c("Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2")
)

matriz_abundancia_teste <- list(
  matriz = matrix(c(
    5, 2, 3, 1,
    1, 4, 2, 0,
    3, 1, 5, 2,
    2, 3, 1, 4
  ), nrow = 4, byrow = TRUE),
  nomes_especies = c("Apis mellifera", "Bombus terrestris", 
                     "Xylocopa violacea", "Megachile rotundata"),
  nomes_amostras = c("Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste")
)

dados_rda_teste <- list(
  especies = matrix(c(
    5, 2, 3,
    1, 4, 2,
    3, 1, 5,
    2, 3, 1
  ), nrow = 4, byrow = TRUE),
  ambiente = matrix(c(
    12.5, 3.2,
    15.1, 2.8,
    10.3, 4.5,
    18.2, 2.1
  ), nrow = 4, byrow = TRUE),
  nomes_especies = c("Apis mellifera", "Bombus terrestris", "Xylocopa violacea"),
  nomes_amostras = c("Área_1", "Área_2", "Área_3", "Área_4"),
  nomes_variaveis_ambientais = c("Temperatura_°C", "pH_Solo")
)

dados_grupos_teste <- list(
  grupo1 = c(12, 15, 14, 10, 13, 16),
  grupo2 = c(18, 20, 19, 22, 21, 17),
  nome_grupo1 = "Área_Controle",
  nome_grupo2 = "Área_Tratamento"
)

dados_correlacao_teste <- list(
  x = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
  y = c(2.1, 4.3, 5.8, 7.9, 10.2, 12.1, 14.5, 16.3, 18.8, 20.5),
  nome_x = "Temperatura_°C",
  nome_y = "Riqueza_Espécies"
)

dados_anova_teste <- list(
  valores = c(12, 15, 14, 18, 20, 19, 22, 21, 25, 10, 11, 13),
  grupos = c("Floresta", "Floresta", "Floresta", "Floresta", 
             "Cerrado", "Cerrado", "Cerrado", "Cerrado", 
             "Pastagem", "Pastagem", "Pastagem", "Pastagem"),
  nome_variavel = "Riqueza_de_Espécies"
)

dados_normalidade_teste <- list(
  dados = rnorm(50, mean = 100, sd = 15),
  nome_variavel = "Abundância_Total"
)

dados_mcnaughton_teste <- list(
  abundancias = c(45, 23, 12, 8, 5, 3, 2, 2, 1, 1, 1),
  nomes_especies = nomes_especies_exemplo
)

dados_glm_teste <- list(
  y = c(5, 12, 8, 15, 3, 18, 7, 20),
  x = c(10, 15, 12, 18, 8, 22, 11, 25),
  nome_y = "Número_de_Espécies",
  nome_x = "Área_ha"
)

# Execução de testes

cat("\n")
cat("Iniciando testes...\n")

# Teste de saúde
cat("\nHealth Check\n")
tryCatch({
  resposta <- GET(paste0(base_url, "/health"))
  if (status_code(resposta) == 200) {
    cat("API está funcionando!\n")
    print(content(resposta))
  }
}, error = function(e) {
  cat("API não está respondendo.\n")
  stop("Não foi possível conectar à API")
})


cat("\n\nTestes:\n")

testar_endpoint("/analise/lognormal", abundancias_teste, "Modelo Log-Normal")
#testar_endpoint("/analise/logserie", abundancias_teste, "Modelo Log-Serie")
#testar_endpoint("/analise/geometrica", abundancias_teste, "Modelo Série Geométrica")
#testar_endpoint("/analise/vara_quebrada", abundancias_teste, "Modelo Vara Quebrada")
#testar_endpoint("/analise/rarefacao", abundancias_teste, "Rarefação")
#testar_endpoint("/analise/jackknife1", abundancias_teste, "Jackknife 1")
#testar_endpoint("/analise/jackknife2", abundancias_teste, "Jackknife 2")
#testar_endpoint("/analise/chao1", abundancias_teste, "Chao 1")
#testar_endpoint("/analise/chao2", matriz_pa_teste, "Chao 2")
#testar_endpoint("/analise/bootstrap", abundancias_teste, "Bootstrap")
#testar_endpoint("/analise/ace", abundancias_teste, "ACE")
#testar_endpoint("/analise/ice", matriz_pa_teste, "ICE")
#testar_endpoint("/analise/shannon", abundancias_teste, "Shannon-Wiener")
#testar_endpoint("/analise/simpson", abundancias_teste, "Simpson")
#testar_endpoint("/analise/margalef", abundancias_teste, "Margalef")
#testar_endpoint("/analise/pielou", abundancias_teste, "Pielou")
#testar_endpoint("/analise/berger_parker", abundancias_teste, "Berger-Parker")
#testar_endpoint("/analise/brillouin", abundancias_teste, "Brillouin")
#testar_endpoint("/analise/macintosh", abundancias_teste, "MacIntosh")
#testar_endpoint("/analise/hurlbert", abundancias_teste, "Hurlbert's PIE")
#testar_endpoint("/analise/mcnaughton", dados_mcnaughton_teste, "McNaughton")
#testar_endpoint("/analise/teste_t", dados_grupos_teste, "Teste T")
#testar_endpoint("/analise/pearson", dados_correlacao_teste, "Correlação de Pearson")
#testar_endpoint("/analise/anova", dados_anova_teste, "ANOVA")
#testar_endpoint("/analise/regressao_linear", dados_correlacao_teste, "Regressão Linear")
#testar_endpoint("/analise/spearman", dados_correlacao_teste, "Spearman")
#testar_endpoint("/analise/kendall", dados_correlacao_teste, "Kendall")
#testar_endpoint("/analise/shapiro", dados_normalidade_teste, "Shapiro-Wilk")
#testar_endpoint("/analise/kruskal", dados_anova_teste, "Kruskal-Wallis")
#testar_endpoint("/analise/rda", dados_rda_teste, "RDA")
#testar_endpoint("/analise/cca", dados_rda_teste, "CCA")
#testar_endpoint("/analise/nmds", matriz_abundancia_teste, "nMDS")
#testar_endpoint("/analise/pca", matriz_abundancia_teste, "PCA")
#testar_endpoint("/analise/jaccard", matriz_pa_teste, "Jaccard")
#testar_endpoint("/analise/bray_curtis", matriz_abundancia_teste, "Bray-Curtis")
#testar_endpoint("/analise/morisita", matriz_abundancia_teste, "Morisita-Horn")
#testar_endpoint("/analise/sorensen", matriz_pa_teste, "Sørensen-Dice")
#testar_endpoint("/analise/modelo_gaussiano", dados_glm_teste, "Modelo Gaussiano")
#testar_endpoint("/analise/modelo_gamma", dados_glm_teste, "Modelo Gamma")
#testar_endpoint("/analise/modelo_poisson", dados_glm_teste, "Modelo Poisson")
#testar_endpoint("/analise/modelo_binomial_negativa", dados_glm_teste, "Modelo Binomial Negativa")

cat("\n\n")
cat("# Testes concluídos.                        #\n")