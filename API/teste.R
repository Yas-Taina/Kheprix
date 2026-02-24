# =============================================================================
# Script de Teste para API de Análises do Sistema Kheprix
# =============================================================================
library(httr)
library(jsonlite)

base_url <- "http://localhost:8000"

# =============================================================================
# Contadores de resultado
# =============================================================================
resultados <- list(sucesso = 0, erro = 0, falha = c())

# =============================================================================
# Função auxiliar para testar endpoints
# =============================================================================
testar_endpoint <- function(endpoint, dados, nome_teste, mostrar_resultado = TRUE) {
  cat("\n====================================\n")
  cat("Testando:", nome_teste, "\n")
  cat("====================================\n")

  tryCatch({
    resposta <- POST(
      url      = paste0(base_url, endpoint),
      body     = toJSON(dados, auto_unbox = TRUE),
      content_type_json()
    )

    status <- status_code(resposta)

    if (status == 200) {
      cat("SUCESSO - Status:", status, "\n")
      resultados$sucesso <<- resultados$sucesso + 1

      content_type <- headers(resposta)$`content-type`

      if (grepl("application/json", content_type)) {
        if (mostrar_resultado) {
          resultado <- content(resposta, as = "parsed")
          print(resultado)
        } else {
          cat("(resultado JSON omitido para brevidade)\n")
        }
      } else if (grepl("text/html", content_type)) {
        html_content <- content(resposta, as = "text", encoding = "UTF-8")
        if (nchar(html_content) > 100) {
          cat("Gráfico HTML gerado com sucesso!\n")
          cat("Tamanho:", nchar(html_content), "caracteres\n")
        } else {
          cat("AVISO: HTML retornado parece muito curto —", nchar(html_content), "caracteres\n")
          cat(html_content, "\n")
        }
      }

    } else {
      cat("ERRO - Status:", status, "\n")
      resultados$erro <<- resultados$erro + 1
      resultados$falha <<- c(resultados$falha, nome_teste)
      tryCatch(print(content(resposta, as = "parsed")), error = function(e) {
        cat("(não foi possível parsear a resposta de erro)\n")
      })
    }

  }, error = function(e) {
    cat("ERRO DE CONEXÃO:", e$message, "\n")
    resultados$erro <<- resultados$erro + 1
    resultados$falha <<- c(resultados$falha, nome_teste)
  })
}

# =============================================================================
# DADOS DE TESTE
# =============================================================================

# --- Abundâncias simples (vetor) ---
nomes_especies_exemplo <- c(
  "Apis mellifera", "Bombus terrestris", "Xylocopa violacea",
  "Megachile rotundata", "Osmia bicornis", "Lasioglossum malachurum",
  "Andrena fulva", "Halictus rubicundus", "Anthophora plumipes",
  "Eucera nigrescens", "Colletes daviesanus"
)

abundancias_teste <- list(
  abundancias    = c(45, 23, 12, 8, 5, 3, 2, 2, 1, 1, 1),
  nomes_especies = nomes_especies_exemplo
)

# --- Presença/Ausência (para Chao2, ICE, Jaccard, Sørensen) ---
dados_pa_teste <- list(
  abundancias_por_amostra = list(
    c(1, 0, 1, 0, 1),
    c(1, 1, 0, 0, 1),
    c(0, 1, 1, 1, 0),
    c(1, 0, 1, 1, 1)
  ),
  nomes_especies = c("Apis mellifera", "Bombus terrestris", "Xylocopa violacea",
                     "Megachile rotundata", "Osmia bicornis"),
  nomes_amostras = c("Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2")
)

# --- Abundâncias por amostra (para Bray-Curtis, Morisita-Horn, nMDS, PCA) ---
dados_abundancia_teste <- list(
  abundancias_por_amostra = list(
    c(5, 2, 3, 1),
    c(1, 4, 2, 0),
    c(3, 1, 5, 2),
    c(2, 3, 1, 4)
  ),
  nomes_especies = c("Apis mellifera", "Bombus terrestris",
                     "Xylocopa violacea", "Megachile rotundata"),
  nomes_amostras = c("Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste")
)

# --- RDA/CCA com 3 variáveis (amostras > variáveis — sem colinearidade) ---
# IMPORTANTE: usar mais amostras do que variáveis para evitar overfitting
dados_rda_cca_teste <- list(
  abundancias_por_amostra = list(
    c(5, 2, 3),
    c(1, 4, 2),
    c(3, 1, 5),
    c(2, 3, 1),
    c(4, 2, 4),
    c(1, 5, 2)
  ),
  variaveis_por_amostra = list(
    c(12.5, 3.2, 65.5),
    c(15.1, 2.8, 72.3),
    c(10.3, 4.5, 58.1),
    c(18.2, 2.1, 80.5),
    c(13.7, 3.8, 70.0),
    c(16.5, 2.5, 75.2)
  ),
  nomes_especies              = c("Apis mellifera", "Bombus terrestris", "Xylocopa violacea"),
  nomes_amostras              = c("Area_1", "Area_2", "Area_3", "Area_4", "Area_5", "Area_6"),
  nomes_variaveis_ambientais  = c("Temperatura_C", "pH_Solo", "Umidade_pct")
)

# --- RDA/CCA com 5 variáveis (mais amostras para evitar overfitting) ---
dados_rda_5vars_teste <- list(
  abundancias_por_amostra = list(
    c(5, 2, 3, 1),
    c(1, 4, 2, 3),
    c(3, 1, 5, 2),
    c(2, 3, 1, 4),
    c(4, 2, 4, 1),
    c(1, 5, 2, 3),
    c(3, 3, 3, 2),
    c(2, 1, 4, 5)
  ),
  variaveis_por_amostra = list(
    c(12.5, 3.2, 65.5, 150.0, 25.0),
    c(15.1, 2.8, 72.3, 180.0, 30.0),
    c(10.3, 4.5, 58.1, 120.0, 20.0),
    c(18.2, 2.1, 80.5, 200.0, 35.0),
    c(13.7, 3.8, 70.0, 160.0, 28.0),
    c(16.5, 2.5, 75.0, 190.0, 32.0),
    c(11.0, 4.0, 60.0, 130.0, 22.0),
    c(17.0, 2.3, 78.0, 195.0, 33.0)
  ),
  nomes_especies             = c("Apis mellifera", "Bombus terrestris",
                                 "Xylocopa violacea", "Megachile rotundata"),
  nomes_amostras             = c("Local_A", "Local_B", "Local_C", "Local_D",
                                 "Local_E", "Local_F", "Local_G", "Local_H"),
  nomes_variaveis_ambientais = c("Temperatura_C", "pH_Solo", "Umidade_pct",
                                 "Altitude_m", "Cobertura_Vegetal_pct")
)

# --- Grupos (Teste T, Kruskal-Wallis, ANOVA) ---
dados_grupos_teste <- list(
  grupo1      = c(12, 15, 14, 10, 13, 16),
  grupo2      = c(18, 20, 19, 22, 21, 17),
  nome_grupo1 = "Area_Controle",
  nome_grupo2 = "Area_Tratamento"
)

dados_anova_teste <- list(
  valores      = c(12, 15, 14, 18, 20, 19, 22, 21, 25, 10, 11, 13),
  grupos       = c("Floresta",  "Floresta",  "Floresta",  "Floresta",
                   "Cerrado",   "Cerrado",   "Cerrado",   "Cerrado",
                   "Pastagem",  "Pastagem",  "Pastagem",  "Pastagem"),
  nome_variavel = "Riqueza_de_Especies"
)

# --- Correlação / Regressão ---
dados_correlacao_teste <- list(
  x      = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
  y      = c(2.1, 4.3, 5.8, 7.9, 10.2, 12.1, 14.5, 16.3, 18.8, 20.5),
  nome_x = "Temperatura_C",
  nome_y = "Riqueza_Especies"
)

# --- Normalidade (Shapiro-Wilk) ---
set.seed(42)  # semente fixa para reprodutibilidade
dados_normalidade_teste <- list(
  dados         = round(rnorm(50, mean = 100, sd = 15), 2),
  nome_variavel = "Abundancia_Total"
)

# --- McNaughton ---
dados_mcnaughton_teste <- list(
  abundancias    = c(45, 23, 12, 8, 5, 3, 2, 2, 1, 1, 1),
  nomes_especies = nomes_especies_exemplo
)

# --- GLM (Gaussiano, Poisson, Binomial Negativa) ---
dados_glm_teste <- list(
  y      = c(5, 12, 8, 15, 3, 18, 7, 20),
  x      = c(10, 15, 12, 18, 8, 22, 11, 25),
  nome_y = "Numero_de_Especies",
  nome_x = "Area_ha"
)

# --- GLM Gamma (variável resposta positiva contínua) ---
dados_glm_gamma_teste <- list(
  y      = c(2.5, 5.2, 3.8, 6.5, 1.3, 8.8, 4.7, 10.2),
  x      = c(10, 15, 12, 18, 8, 22, 11, 25),
  nome_y = "Biomassa_kg",
  nome_x = "Area_ha"
)

# --- Kolmogorov-Smirnov ---
dados_ks_teste <- list(
  amostra1      = c(12, 15, 14, 10, 13, 16, 11, 14, 15, 12),
  amostra2      = c(18, 20, 19, 22, 21, 17, 19, 20, 18, 21),
  nome_amostra1 = "Local_A",
  nome_amostra2 = "Local_B"
)

# --- Michaelis-Menten (matriz de acumulação, amostras em linhas) ---
matriz_acumulacao_teste <- list(
  matriz = matrix(c(
    5, 2, 3, 1, 0, 0,
    5, 3, 3, 1, 1, 0,
    5, 3, 4, 2, 1, 0,
    5, 3, 4, 2, 1, 1
  ), nrow = 4, byrow = TRUE)
)

# =============================================================================
# EXECUÇÃO DOS TESTES
# =============================================================================

cat("\n")
cat("=============================================================================\n")
cat("  Iniciando testes da API Kheprix\n")
cat("=============================================================================\n")

# --- Health Check ---
cat("\n[Health Check]\n")
tryCatch({
  resposta <- GET(paste0(base_url, "/health"))
  if (status_code(resposta) == 200) {
    cat("API está funcionando! Status: 200\n")
  } else {
    cat("AVISO: Health check retornou status", status_code(resposta), "\n")
  }
}, error = function(e) {
  cat("ERRO CRÍTICO: API não está respondendo —", e$message, "\n")
  stop("Interrompendo testes: não foi possível conectar à API.")
})

cat("\n\n--- Modelos de Distribuição de Abundância ---\n")
testar_endpoint("/analise/lognormal",    abundancias_teste, "Modelo Log-Normal")
testar_endpoint("/analise/logserie",     abundancias_teste, "Modelo Log-Serie")
testar_endpoint("/analise/geometrica",   abundancias_teste, "Modelo Serie Geometrica")
testar_endpoint("/analise/vara_quebrada",abundancias_teste, "Modelo Vara Quebrada")

cat("\n\n--- Curvas de Acumulação e Rarefação ---\n")
testar_endpoint("/analise/rarefacao",       abundancias_teste,       "Rarefacao")
testar_endpoint("/analise/michaelis_menten",matriz_acumulacao_teste, "Michaelis-Menten")

cat("\n\n--- Estimadores de Riqueza ---\n")
testar_endpoint("/analise/jackknife1", abundancias_teste, "Jackknife 1")
testar_endpoint("/analise/jackknife2", abundancias_teste, "Jackknife 2")
testar_endpoint("/analise/chao1",      abundancias_teste, "Chao 1")
testar_endpoint("/analise/chao2",      dados_pa_teste,    "Chao 2")
testar_endpoint("/analise/bootstrap",  abundancias_teste, "Bootstrap")
testar_endpoint("/analise/ace",        abundancias_teste, "ACE")
testar_endpoint("/analise/ice",        dados_pa_teste,    "ICE")

cat("\n\n--- Índices de Diversidade ---\n")
testar_endpoint("/analise/shannon",      abundancias_teste,      "Shannon-Wiener")
testar_endpoint("/analise/simpson",      abundancias_teste,      "Simpson")
testar_endpoint("/analise/margalef",     abundancias_teste,      "Margalef")
testar_endpoint("/analise/pielou",       abundancias_teste,      "Pielou")
testar_endpoint("/analise/berger_parker",abundancias_teste,      "Berger-Parker")
testar_endpoint("/analise/brillouin",    abundancias_teste,      "Brillouin")
testar_endpoint("/analise/macintosh",    abundancias_teste,      "MacIntosh")
testar_endpoint("/analise/hurlbert",     abundancias_teste,      "Hurlbert PIE")
testar_endpoint("/analise/mcnaughton",   dados_mcnaughton_teste, "McNaughton")

cat("\n\n--- Índices de Similaridade / Dissimilaridade ---\n")
testar_endpoint("/analise/jaccard",          dados_pa_teste,        "Jaccard")
testar_endpoint("/analise/jaccard_grafico",  dados_pa_teste,        "Jaccard (Grafico)")
testar_endpoint("/analise/sorensen",         dados_pa_teste,        "Sorensen-Dice")
testar_endpoint("/analise/sorensen_grafico", dados_pa_teste,        "Sorensen-Dice (Grafico)")
testar_endpoint("/analise/bray_curtis",      dados_abundancia_teste,"Bray-Curtis")
testar_endpoint("/analise/bray_curtis_grafico",dados_abundancia_teste,"Bray-Curtis (Grafico)")
testar_endpoint("/analise/morisita",         dados_abundancia_teste,"Morisita-Horn")
testar_endpoint("/analise/morisita_grafico", dados_abundancia_teste,"Morisita-Horn (Grafico)")

cat("\n\n--- Análises Multivariadas ---\n")
testar_endpoint("/analise/rda",  dados_rda_cca_teste,  "RDA com 3 variaveis (6 amostras)")
testar_endpoint("/analise/rda",  dados_rda_5vars_teste,"RDA com 5 variaveis (8 amostras)")
testar_endpoint("/analise/cca",  dados_rda_cca_teste,  "CCA com 3 variaveis (6 amostras)")
testar_endpoint("/analise/cca",  dados_rda_5vars_teste,"CCA com 5 variaveis (8 amostras)")
testar_endpoint("/analise/nmds", dados_abundancia_teste,"nMDS (Grafico)")
testar_endpoint("/analise/pca",  dados_abundancia_teste,"PCA (Grafico)")

cat("\n\n--- Correlações ---\n")
testar_endpoint("/analise/pearson",         dados_correlacao_teste,"Correlacao de Pearson")
testar_endpoint("/analise/pearson_grafico", dados_correlacao_teste,"Correlacao de Pearson (Grafico)")
testar_endpoint("/analise/spearman",        dados_correlacao_teste,"Spearman")
testar_endpoint("/analise/spearman_grafico",dados_correlacao_teste,"Spearman (Grafico)")
testar_endpoint("/analise/kendall",         dados_correlacao_teste,"Kendall")
testar_endpoint("/analise/kendall_grafico", dados_correlacao_teste,"Kendall (Grafico)")
testar_endpoint("/analise/regressao_linear",dados_correlacao_teste,"Regressao Linear")

cat("\n\n--- Testes Estatísticos ---\n")
testar_endpoint("/analise/teste_t", dados_grupos_teste,    "Teste T")
testar_endpoint("/analise/shapiro", dados_normalidade_teste,"Shapiro-Wilk")
testar_endpoint("/analise/kruskal", dados_anova_teste,     "Kruskal-Wallis")
testar_endpoint("/analise/anova",   dados_anova_teste,     "ANOVA")
testar_endpoint("/analise/ks",      dados_ks_teste,        "Kolmogorov-Smirnov")

cat("\n\n--- Modelos GLM ---\n")
testar_endpoint("/analise/modelo_gaussiano",        dados_glm_teste,       "Modelo Gaussiano")
testar_endpoint("/analise/modelo_gaussiano_grafico",dados_glm_teste,       "Modelo Gaussiano (Grafico)")
testar_endpoint("/analise/modelo_gamma",            dados_glm_gamma_teste, "Modelo Gamma")
testar_endpoint("/analise/modelo_gamma_grafico",    dados_glm_gamma_teste, "Modelo Gamma (Grafico)")
testar_endpoint("/analise/modelo_poisson",          dados_glm_teste,       "Modelo Poisson")
testar_endpoint("/analise/modelo_poisson_grafico",  dados_glm_teste,       "Modelo Poisson (Grafico)")
testar_endpoint("/analise/modelo_binomial_negativa",dados_glm_teste,       "Modelo Binomial Negativa")
testar_endpoint("/analise/modelo_binomial_grafico", dados_glm_teste,       "Modelo Binomial Negativa (Grafico)")

# =============================================================================
# RESUMO FINAL
# =============================================================================
cat("\n\n")
cat("=============================================================================\n")
cat("  RESUMO DOS TESTES\n")
cat("=============================================================================\n")
cat("  Sucesso : ", resultados$sucesso, "\n")
cat("  Erro    : ", resultados$erro, "\n")
total <- resultados$sucesso + resultados$erro
cat("  Total   : ", total, "\n")

if (resultados$erro > 0) {
  cat("\n  Endpoints com falha:\n")
  for (f in resultados$falha) {
    cat("    -", f, "\n")
  }
} else {
  cat("\n  Todos os endpoints responderam com sucesso!\n")
}
cat("=============================================================================\n\n")