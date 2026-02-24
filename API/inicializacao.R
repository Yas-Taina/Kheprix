# Script de Inicialização da API de Análises do sistema Kheprix
# Execute com: Rscript inicializacao.R

# Lista de pacotes necessários
pacotes <- c(
  "plumber",
  "vegan",
  "ggplot2",
  "plotly",
  "htmlwidgets",
  "jsonlite",
  "dplyr",
  "tidyr",
  "MASS"
)

cat("Verificando e instalando pacotes necessários...\n")

# Instalar pacotes faltantes
instalar <- pacotes[!(pacotes %in% installed.packages()[, "Package"])]
if(length(instalar)) {
  cat("Instalando:", paste(instalar, collapse = ", "), "\n")
  install.packages(instalar, repos = "https://cloud.r-project.org")
}

cat("\nCarregando pacotes...\n")

# Carregar pacotes
carregar_pacote <- function(pkg) {
  tryCatch({
    suppressPackageStartupMessages(library(pkg, character.only = TRUE))
    cat("Ok ", pkg, "\n")
    TRUE
  }, error = function(e) {
    cat(pkg, "ERRO:", e$message, "\n")
    FALSE
  })
}

resultados <- sapply(pacotes, carregar_pacote)

if(!all(resultados)) {
  cat("\n Alguns pacotes não foram carregados.\n")
  cat("A API pode não funcionar completamente.\n\n")
}

# Verificar se o arquivo da API existe
if(!file.exists("plumber.R")) {
  cat("\n Arquivo 'plumber.R' não encontrado\n")
  stop("Arquivo da API não encontrado")
}

cat("Carregando API...\n")

# Carregar API
tryCatch({
  api <- plumb("plumber.R")
  
  cat("\nAPI carregada com sucesso!\n\n")

  cat("Iniciando servidor na porta 8000...\n")
  cat("==============================================\n")
  cat("Endpoint de saúde:\n")
  cat("   http://localhost:8000/health\n\n")
  
  # Iniciar API
  api$run(port = 8000, host = "0.0.0.0", swagger = TRUE)
  
}, error = function(e) {
  cat("\nERRO ao carregar a API:\n")
  cat(e$message, "\n\n")
  stop("Falha ao iniciar API")
})