# API de Análises para Sistema Kheprix

#Define diretório temporário para evitar problemas com pathnames longos no Windows
dir_temp <- "C:/temp"
if (!dir.exists(dir_temp)) dir.create(dir_temp)
Sys.setenv(TMPDIR = dir_temp)
Sys.setenv(TEMP = dir_temp)
Sys.setenv(TMP = dir_temp)

#Inicialização de bibliotecas
library(plumber)
library(vegan)
library(ggplot2)
library(plotly)
library(htmlwidgets)
library(jsonlite)
library(dplyr)
library(tidyr)
library(MASS)

#Função para salvar gráficos em html
salvar_grafico <- function(p) {
  fig <- ggplotly(p)
  json_txt <- plotly::plotly_json(fig, jsonedit = FALSE)
  pj <- jsonlite::fromJSON(json_txt, simplifyVector = FALSE)
  html <- paste0(
    "<!DOCTYPE html>",
    "<html>",
    "<head>",
    "<meta charset='utf-8'>",
    "<meta name='viewport' content='width=device-width, initial-scale=1'>",
    "<script src='https://cdn.plot.ly/plotly-2.24.1.min.js'></script>",
    "</head>",
    "<body style='margin:0;'>",
    "<div id='grafico' style='width:100%;height:100%;'></div>",
    "<script>",
    "var fig = ", jsonlite::toJSON(pj, auto_unbox = TRUE), ";",
    "Plotly.newPlot('grafico', fig.data, fig.layout, fig.config);",
    "</script>",
    "</body></html>"
  )
  return(html)
}

#Função radfit customizada (evita uso da bilbioteca BiodiversityR)
radfit_custom <- function(abundancias) {
  y <- sort(abundancias, decreasing = TRUE)
  x <- seq_along(y)
  log_y <- log(y)
  
 fit_lognormal <- tryCatch({
  fit_tmp <- nls(log_y ~ a - (x - b)^2 / (2 * c^2),
      start = list(a = max(log_y), b = 1, c = length(y)/2),
      control = nls.control(maxiter = 100, warnOnly = TRUE))
  if (isTRUE(fit_tmp$convInfo$isConv)) fit_tmp else NULL
  }, error = function(e) NULL)

  if (is.null(fit_lognormal)) {
    a_est <- max(log_y)
    b_est <- which.max(log_y)
    c_est <- sd(log_y) * length(y) / 4
    predicted_ln <- exp(a_est - (x - b_est)^2 / (2 * c_est^2))
    if (all(is.finite(predicted_ln)) && !all(predicted_ln == 0)) {
      fit_lognormal <- list(fitted = predicted_ln)
    }
  }


  fit_logseries <- tryCatch({
    N <- sum(y)
    S <- length(y)
    fisher_result <- fisherfit(y)
    alpha <- fisher_result$estimate
    x_param <- N / (N + alpha)
    predicted <- sapply(x, function(rank) {
      -alpha * log(1 - x_param) * (x_param^rank) / rank
    })
    if(all(is.finite(predicted)) && !all(predicted == 0)) {
      list(fitted = predicted, alpha = alpha, x = x_param)
    } else {
      NULL
    }
  }, error = function(e) {
    cat("Erro no ajuste log-serie:", conditionMessage(e), "\n")
    NULL
  })
  
  fit_geometric <- tryCatch({
    k <- exp(-coef(lm(log_y ~ x))[2])
    a <- exp(coef(lm(log_y ~ x))[1])
    predicted <- a * k^(x - 1)
    list(fitted = predicted)
  }, error = function(e) NULL)
  
  fit_brokenstick <- tryCatch({
    S <- length(y)
    N <- sum(y)
    predicted <- (N/S) * sapply(x, function(j) sum(1/seq(j, S)))
    list(fitted = predicted)
  }, error = function(e) NULL)
  
  result <- list(
    y = y,
    models = list(
      lognormal = fit_lognormal,
      logseries = fit_logseries,
      geometric = fit_geometric,
      brokenstick = fit_brokenstick
    )
  )
  
  class(result) <- "radfit_custom"
  return(result)
}

fitted.radfit_custom <- function(object, ...) {
  y <- object$y
  n <- length(y)
  
  result <- matrix(NA, nrow = n, ncol = 4)
  colnames(result) <- c("lognormal", "logseries", "geometric", "brokenstick")
  
  # Log-normal
  if (!is.null(object$models$lognormal)) {
    if (is.list(object$models$lognormal) && "fitted" %in% names(object$models$lognormal)) {
      result[, "lognormal"] <- object$models$lognormal$fitted
    } else {
      result[, "lognormal"] <- exp(predict(object$models$lognormal))
    }
  }
  
  # Log-series
  if(!is.null(object$models$logseries)) {
    if(is.list(object$models$logseries) && "fitted" %in% names(object$models$logseries)) {
      fitted_vals <- object$models$logseries$fitted
      if(length(fitted_vals) == n) {
        result[, "logseries"] <- fitted_vals
      } else {
        cat("AVISO: Log-series tem tamanho errado:", length(fitted_vals), "vs", n, "\n")
      }
    }
  }
  
  # Geométrica
  if(!is.null(object$models$geometric)) {
    result[, "geometric"] <- object$models$geometric$fitted
  }
  
  # Vara Quebrada
  if(!is.null(object$models$brokenstick)) {
    result[, "brokenstick"] <- object$models$brokenstick$fitted
  }
  
  return(result)
}

#Função para criar Matriz Presença Ausência
criar_matriz_pa <- function(abundancias_por_amostra, nomes_especies = NULL, nomes_amostras = NULL) {
  if (is.matrix(abundancias_por_amostra)) {
    matriz <- abundancias_por_amostra
  } else if (is.list(abundancias_por_amostra)) {
    matriz <- do.call(rbind, abundancias_por_amostra)
  } else {

    matriz <- as.matrix(abundancias_por_amostra)
  }
  
  matriz_pa <- ifelse(matriz > 0, 1, 0)
  if (!is.null(nomes_especies)) {
    colnames(matriz_pa) <- nomes_especies
  } else {
    colnames(matriz_pa) <- paste0("Sp", 1:ncol(matriz_pa))
  }
  
  if (!is.null(nomes_amostras)) {
    rownames(matriz_pa) <- nomes_amostras
  } else {
    rownames(matriz_pa) <- paste0("Amostra", 1:nrow(matriz_pa))
  }
  
  return(matriz_pa)
}


#Função para criar Matriz de Abundância
criar_matriz_abundancia <- function(abundancias_por_amostra, nomes_especies = NULL, nomes_amostras = NULL) {
  if (is.matrix(abundancias_por_amostra)) {
    matriz <- abundancias_por_amostra
  } else if (is.list(abundancias_por_amostra)) {
    matriz <- do.call(rbind, abundancias_por_amostra)
  } else {
    matriz <- as.matrix(abundancias_por_amostra)
  }
  
  if (!is.null(nomes_especies)) {
    colnames(matriz) <- nomes_especies
  } else {
    colnames(matriz) <- paste0("Sp", 1:ncol(matriz))
  }
  
  if (!is.null(nomes_amostras)) {
    rownames(matriz) <- nomes_amostras
  } else {
    rownames(matriz) <- paste0("Amostra", 1:nrow(matriz))
  }
  
  return(matriz)
}

#Função para criar Matriz de Váriaveis ambientais
criar_matriz_ambiental <- function(variaveis_por_amostra, nomes_variaveis = NULL, nomes_amostras = NULL) {
  if (is.matrix(variaveis_por_amostra)) {
    matriz <- variaveis_por_amostra
  } else if (is.list(variaveis_por_amostra)) {
    matriz <- do.call(rbind, variaveis_por_amostra)
  } else {
    matriz <- as.matrix(variaveis_por_amostra)
  }
  
  if (!is.null(nomes_variaveis)) {
    colnames(matriz) <- nomes_variaveis
  } else {
    colnames(matriz) <- paste0("Var", 1:ncol(matriz))
  }
  
  if (!is.null(nomes_amostras)) {
    rownames(matriz) <- nomes_amostras
  } else {
    rownames(matriz) <- paste0("Amostra", 1:nrow(matriz))
  }
  
  return(matriz)
}

#Filtro CORS para permitir requisições
#' @filter cors
cors <- function(req, res) {
  res$setHeader("Access-Control-Allow-Origin", "*")
  if (req$REQUEST_METHOD == "OPTIONS") {
    res$setHeader("Access-Control-Allow-Methods", "*")
    res$setHeader("Access-Control-Allow-Headers", req$HTTP_ACCESS_CONTROL_REQUEST_HEADERS)
    res$status <- 200
    return(list())
  } else {
    plumber::forward()
  }
}

#' Health check
#' @get /health
function() {
  return(list(status = "API de Análise funcionando", timestamp = Sys.time()))
}

#' Modelo Log-Normal
#' @post /analise/lognormal
#' @serializer html
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie) + nomes científicos opcionais
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8], "nomes_especies": ["Apis mellifera", "Bombus terrestris", ...]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    nomes_especies <- if(!is.null(dados$nomes_especies)) dados$nomes_especies else paste0("Sp", 1:length(abundancias))
    
    fit <- radfit_custom(abundancias)
    
    df_ordenado <- data.frame(
      especie = nomes_especies,
      abundance = abundancias
    ) %>% arrange(desc(abundance)) %>%
      mutate(rank = row_number())
    
    pred_values <- fitted(fit)
    df_ordenado$fitted <- pred_values[, "lognormal"]
    
    p <- ggplot(df_ordenado, aes(x = rank, y = abundance)) +
      geom_point(aes(text = paste("Espécie:", especie, "<br>Abundância:", abundance)), 
                 size = 3, color = "#2C3E50") +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2, na.rm = TRUE) +
      scale_y_log10() +
      labs(title = "Modelo Log-Normal de Abundância de Espécies",
           x = "Rank de Abundância", 
           y = "Abundância (escala log)",
           caption = paste("Análise de", length(abundancias), "espécies")) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Log-Serie
#' @post /analise/logserie
#' @serializer html
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie) + nomes científicos opcionais
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8], "nomes_especies": ["Apis mellifera", "Bombus terrestris", ...]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    nomes_especies <- if(!is.null(dados$nomes_especies)) dados$nomes_especies else paste0("Sp", 1:length(abundancias))
    
    fit <- radfit_custom(abundancias)
    
    df_ordenado <- data.frame(
      especie = nomes_especies,
      abundance = abundancias
    ) %>% arrange(desc(abundance)) %>%
      mutate(rank = row_number())
    
    pred_values <- fitted(fit)
    
    logseries_fitted <- pred_values[, "logseries"]
    
    #Método alternativo para gerar ajuste em caso de falha
    if(all(is.na(logseries_fitted)) || all(is.infinite(logseries_fitted))) {   
      N <- sum(abundancias)
      S <- length(abundancias[abundancias > 0])
      
      alpha_est <- tryCatch({
        fisher_result <- fisherfit(abundancias)
        fisher_result$estimate
      }, error = function(e) {
        N / (N/S - 1)
      })
      
      x <- N / (N + alpha_est)
      
      ranks <- 1:length(abundancias)
      logseries_fitted <- -alpha_est * log(1 - x) * (x^ranks) / ranks
    }
    
    df_ordenado$fitted <- logseries_fitted
    
    p <- ggplot(df_ordenado, aes(x = rank, y = abundance)) +
      geom_point(aes(text = paste("Espécie:", especie, "<br>Abundância:", abundance)), 
                 size = 3, color = "#2C3E50") +
      geom_line(aes(y = fitted), color = "#3498DB", linewidth = 1.2, na.rm = TRUE) +
      scale_y_log10() +
      labs(title = "Modelo Log-Serie de Abundância de Espécies",
           x = "Rank de Abundância", 
           y = "Abundância (escala log)",
           caption = paste("Análise de", length(abundancias), "espécies")) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14))
    
    grafico <- ggplotly(p)

    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Série Geométrica
#' @post /analise/geometrica
#' @serializer html
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie) + nomes científicos opcionais
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8], "nomes_especies": ["Apis mellifera", "Bombus terrestris", ...]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    nomes_especies <- if(!is.null(dados$nomes_especies)) dados$nomes_especies else paste0("Sp", 1:length(abundancias))
    
    fit <- radfit_custom(abundancias)
    
    df_ordenado <- data.frame(
      especie = nomes_especies,
      abundance = abundancias
    ) %>% arrange(desc(abundance)) %>%
      mutate(rank = row_number())
    
    pred_values <- fitted(fit)
    df_ordenado$fitted <- pred_values[, "geometric"]
    
    p <- ggplot(df_ordenado, aes(x = rank, y = abundance)) +
      geom_point(aes(text = paste("Espécie:", especie, "<br>Abundância:", abundance)), 
                 size = 3, color = "#2C3E50") +
      geom_line(aes(y = fitted), color = "#27AE60", linewidth = 1.2, na.rm = TRUE) +
      scale_y_log10() +
      labs(title = "Modelo Série Geométrica de Abundância de Espécies",
           x = "Rank de Abundância", 
           y = "Abundância (escala log)",
           caption = paste("Análise de", length(abundancias), "espécies")) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Vara Quebrada
#' @post /analise/vara_quebrada
#' @serializer html
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie) + nomes científicos opcionais
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8], "nomes_especies": ["Apis mellifera", "Bombus terrestris", ...]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    nomes_especies <- if(!is.null(dados$nomes_especies)) dados$nomes_especies else paste0("Sp", 1:length(abundancias))
    
    fit <- radfit_custom(abundancias)
    
    df_ordenado <- data.frame(
      especie = nomes_especies,
      abundance = abundancias
    ) %>% arrange(desc(abundance)) %>%
      mutate(rank = row_number())
    
    pred_values <- fitted(fit)
    df_ordenado$fitted <- pred_values[, "brokenstick"]
    
    p <- ggplot(df_ordenado, aes(x = rank, y = abundance)) +
      geom_point(aes(text = paste("Espécie:", especie, "<br>Abundância:", abundance)), 
                 size = 3, color = "#2C3E50") +
      geom_line(aes(y = fitted), color = "#9B59B6", linewidth = 1.2, na.rm = TRUE) +
      scale_y_log10() +
      labs(title = "Modelo Vara Quebrada de Abundância de Espécies",
           x = "Rank de Abundância", 
           y = "Abundância (escala log)",
           caption = paste("Análise de", length(abundancias), "espécies")) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Rarefação
#' @post /analise/rarefacao
#' @serializer html
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    n_total <- sum(abundancias)
    n_especies <- sum(abundancias > 0)
    
    subsample <- unique(round(seq(1, n_total, length.out = min(50, n_total))))
    
    rarefied <- sapply(subsample, function(n) {
      rarefy(matrix(abundancias, nrow = 1), sample = n)
    })
    
    p <- ggplot(data.frame(sample_size = subsample, richness = rarefied),
                aes(x = sample_size, y = richness)) +
      geom_line(color = "#E67E22", linewidth = 1.2) +
      geom_point(size = 2, color = "#E67E22") +
      geom_hline(yintercept = n_especies, linetype = "dashed", color = "gray50") +
      annotate("text", x = n_total * 0.7, y = n_especies + 0.5,
               label = paste("Riqueza Observada:", n_especies), color = "gray50") +
      labs(title = "Curva de Rarefação",
           x = "Tamanho da Amostra (n° de indivíduos)", 
           y = "Riqueza Esperada (n° de espécies)",
           caption = paste("Total de indivíduos:", n_total, "| Espécies observadas:", n_especies)) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Jackknife 1
#' @post /analise/jackknife1
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    S_obs <- sum(abundancias > 0)
    L <- sum(abundancias == 1)
    m <- length(abundancias)
    
    jack1 <- S_obs + L * ((m - 1) / m)
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_jack1 = round(jack1, 2),
      especies_unicas = L,
      especies_nao_detectadas_estimadas = round(jack1 - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Jackknife 2
#' @post /analise/jackknife2
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    S_obs <- sum(abundancias > 0)
    L <- sum(abundancias == 1)
    M <- sum(abundancias == 2)
    m <- length(abundancias)
    
    jack2 <- S_obs + L * ((2*m - 3) / m) - M * ((m - 2)^2 / (m * (m - 1)))
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_jack2 = round(jack2, 2),
      especies_unicas = L,
      especies_duplicadas = M,
      especies_nao_detectadas_estimadas = round(jack2 - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Chao 1
#' @post /analise/chao1
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    S_obs <- sum(abundancias > 0)
    f1 <- sum(abundancias == 1)
    f2 <- sum(abundancias == 2)
    
    if (f2 > 0) {
      chao1 <- S_obs + (f1^2) / (2 * f2)
    } else {
      chao1 <- S_obs + (f1 * (f1 - 1)) / 2
    }
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_chao1 = round(chao1, 2),
      singletons = f1,
      doubletons = f2,
      especies_nao_detectadas_estimadas = round(chao1 - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Chao 2
#' @post /analise/chao2
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra 
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    S_obs <- ncol(matriz_pa)
    Q1 <- sum(colSums(matriz_pa) == 1)
    Q2 <- sum(colSums(matriz_pa) == 2)
    m <- nrow(matriz_pa)

    if (Q2 > 0) {
      chao2 <- S_obs + ((m - 1) / m) * (Q1^2 / (2 * Q2))
    } else {
      chao2 <- S_obs + ((m - 1) / m) * (Q1 * (Q1 - 1) / 2)
    }
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_chao2 = round(chao2, 2),
      especies_unicas = Q1,
      especies_duplicadas = Q2,
      numero_amostras = m,
      especies_nao_detectadas_estimadas = round(chao2 - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Bootstrap
#' @post /analise/bootstrap
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    S_obs <- sum(abundancias > 0)
    n <- sum(abundancias)
    
    p_hat <- abundancias / n
    bootstrap_est <- S_obs + sum((1 - p_hat)^n)
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_bootstrap = round(bootstrap_est, 2),
      especies_nao_detectadas_estimadas = round(bootstrap_est - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' ACE (Abundance-based Coverage Estimator)
#' @post /analise/ace
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    ace_result <- estimateR(abundancias)["S.ACE"]
    
    return(list(
      riqueza_observada = sum(abundancias > 0),
      riqueza_estimada_ace = round(as.numeric(ace_result), 2),
      especies_nao_detectadas_estimadas = round(as.numeric(ace_result) - sum(abundancias > 0), 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' ICE (Incidence Coverage Estimator)
#' @post /analise/ice
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
     S_obs <- ncol(matriz_pa)
    freq <- colSums(matriz_pa)
    S_infreq <- sum(freq <= 10)
    S_freq <- sum(freq > 10)
    
    if (S_infreq > 0) {
      C_ice <- 1 - (sum(freq == 1) / sum(freq[freq <= 10]))
      gamma_ice <- max(0, (S_infreq / C_ice) * sum(freq[freq <= 10] * (freq[freq <= 10] - 1)) / 
                         (sum(freq[freq <= 10]) * (sum(freq[freq <= 10]) - 1)) - 1)
      ice <- S_freq + S_infreq / C_ice + sum(freq == 1) / C_ice * gamma_ice
    } else {
      ice <- S_obs
    }
    
    return(list(
      riqueza_observada = S_obs,
      riqueza_estimada_ice = round(ice, 2),
      especies_nao_detectadas_estimadas = round(ice - S_obs, 2)
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Shannon-Wiener
#' @post /analise/shannon
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    shannon <- diversity(abundancias, index = "shannon")
    shannon_max <- log(sum(abundancias > 0))
    
    return(list(
      indice_shannon = round(shannon, 4),
      shannon_maximo = round(shannon_max, 4),
      equitabilidade_relativa = round(shannon / shannon_max, 4),
      riqueza = sum(abundancias > 0),
      interpretacao = "Valores próximos a 0 indicam baixa diversidade; valores altos indicam alta diversidade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Simpson
#' @post /analise/simpson
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    simpson <- diversity(abundancias, index = "simpson")
    inv_simpson <- diversity(abundancias, index = "invsimpson")
    
    return(list(
      indice_simpson = round(simpson, 4),
      diversidade_simpson = round(1 - simpson, 4),
      inverso_simpson = round(inv_simpson, 4),
      interpretacao = "Valores próximos a 1 indicam alta diversidade; próximos a 0 indicam baixa diversidade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Margalef
#' @post /analise/margalef
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    S <- sum(abundancias > 0)
    N <- sum(abundancias)
    margalef <- (S - 1) / log(N)
    
    return(list(
      indice_margalef = round(margalef, 4),
      riqueza = S,
      total_individuos = N,
      interpretacao = "Índice de riqueza que considera o número de espécies e total de indivíduos"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Pielou (Equitabilidade)
#' @post /analise/pielou
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    H <- diversity(abundancias, index = "shannon")
    S <- sum(abundancias > 0)
    J <- H / log(S)
    
    return(list(
      indice_pielou = round(J, 4),
      shannon = round(H, 4),
      riqueza = S,
      interpretacao = "Valores próximos a 1 indicam distribuição uniforme; próximos a 0 indicam dominância de poucas espécies"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Berger-Parker
#' @post /analise/berger_parker
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    N <- sum(abundancias)
    N_max <- max(abundancias)
    bp <- N_max / N
    
    return(list(
      indice_berger_parker = round(bp, 4),
      abundancia_maxima = N_max,
      total_individuos = N,
      proporcao_especie_dominante = paste0(round(bp * 100, 2), "%"),
      interpretacao = "Medida de dominância - valores altos indicam que uma espécie domina a comunidade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Teste T
#' @post /analise/teste_t
#' @serializer json
#' Dados aceitos: Dois vetores numéricos (variáveis contínuas) + labels opcionais
#' Formato esperado: {"grupo1": [12, 15, 14, 10], "grupo2": [18, 20, 19, 22], "nome_grupo1": "Controle", "nome_grupo2": "Tratamento"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    grupo1 <- as.numeric(dados$grupo1)
    grupo2 <- as.numeric(dados$grupo2)
    nome_g1 <- if(!is.null(dados$nome_grupo1)) dados$nome_grupo1 else "Grupo 1"
    nome_g2 <- if(!is.null(dados$nome_grupo2)) dados$nome_grupo2 else "Grupo 2"
    
    teste <- t.test(grupo1, grupo2)
    
    return(list(
      estatistica_t = round(teste$statistic, 4),
      p_valor = round(teste$p.value, 4),
      significativo = teste$p.value < 0.05,
      intervalo_confianca = round(teste$conf.int, 4),
      media_grupo1 = round(mean(grupo1), 4),
      media_grupo2 = round(mean(grupo2), 4),
      diferenca_medias = round(mean(grupo1) - mean(grupo2), 4),
      nome_grupo1 = nome_g1,
      nome_grupo2 = nome_g2
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Correlação de Pearson
#' @post /analise/pearson
#' @serializer json
#' Dados aceitos: Dois vetores numéricos (variáveis contínuas) + labels opcionais
#' Formato esperado: {"x": [1, 2, 3, 4, 5], "y": [2, 4, 5, 4, 6], "nome_x": "Temperatura", "nome_y": "Abundância"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "pearson")
    
    return(list(
      correlacao = round(teste$estimate, 4),
      p_valor = round(teste$p.value, 4),
      significativo = teste$p.value < 0.05,
      intervalo_confianca = round(teste$conf.int, 4),
      interpretacao = ifelse(abs(teste$estimate) > 0.7, "Correlação forte",
                            ifelse(abs(teste$estimate) > 0.4, "Correlação moderada", "Correlação fraca")),
      direcao = ifelse(teste$estimate > 0, "Positiva", "Negativa"),
      nome_x = nome_x,
      nome_y = nome_y
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Pearson - Gráfico
#' @post /analise/pearson_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "pearson")
    
    df_plot <- data.frame(x = x, y = y)
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#3498DB", alpha = 0.7) +
      geom_smooth(method = "lm", se = TRUE, color = "#E74C3C", fill = "#E74C3C", alpha = 0.2) +
      labs(title = paste0("Correlação de Pearson: ", nome_y, " vs ", nome_x),
           subtitle = paste0("r = ", round(teste$estimate, 3),
                           " | p = ", ifelse(teste$p.value < 0.001, "< 0.001", round(teste$p.value, 3)),
                           " | ", ifelse(teste$p.value < 0.05, "Significativo", "Não significativo")),
           x = nome_x,
           y = nome_y,
           caption = ifelse(abs(teste$estimate) > 0.7, "Correlação forte",
                          ifelse(abs(teste$estimate) > 0.4, "Correlação moderada", "Correlação fraca"))) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' ANOVA
#' @post /analise/anova
#' @serializer json
#' Dados aceitos: Vetor de valores e vetor de grupos + labels opcionais
#' Formato esperado: {"valores": [12, 15, 14, 18, 20, 19], "grupos": ["A", "A", "B", "B", "C", "C"], "nome_variavel": "Riqueza"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    valores <- as.numeric(dados$valores)
    grupos <- as.factor(dados$grupos)
    nome_var <- if(!is.null(dados$nome_variavel)) dados$nome_variavel else "Variável"
    
    modelo <- aov(valores ~ grupos)
    resultado <- summary(modelo)
    
    medias <- aggregate(valores ~ grupos, FUN = mean)
    
    return(list(
      f_valor = round(resultado[[1]]$`F value`[1], 4),
      p_valor = round(resultado[[1]]$`Pr(>F)`[1], 4),
      significativo = resultado[[1]]$`Pr(>F)`[1] < 0.05,
      gl_tratamento = resultado[[1]]$Df[1],
      gl_residuo = resultado[[1]]$Df[2],
      medias_por_grupo = setNames(round(medias$valores, 2), medias$grupos),
      nome_variavel = nome_var,
      interpretacao = "Se p < 0.05, há diferença significativa entre pelo menos dois grupos"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Regressão Linear
#' @post /analise/regressao_linear
#' @serializer html
#' Dados aceitos: Dois vetores numéricos (variável preditora e resposta) + labels
#' Formato esperado: {"x": [1, 2, 3, 4, 5], "y": [2, 4, 5, 4, 6], "nome_x": "Temperatura", "nome_y": "Abundância"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável Preditora"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Resposta"
    
    modelo <- lm(y ~ x)
    r2 <- summary(modelo)$r.squared
    p_val <- summary(modelo)$coefficients[2, 4]
    
    intercepto <- coef(modelo)[1]
    inclinacao <- coef(modelo)[2]
    equacao <- paste0("y = ", round(intercepto, 2), " + ", round(inclinacao, 2), "x")
    df_plot <- data.frame(x = x, y = y)
    df_plot$fitted <- fitted(modelo)
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#3498DB", alpha = 0.7) +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      geom_ribbon(aes(ymin = fitted - 1.96*sd(residuals(modelo)), 
                      ymax = fitted + 1.96*sd(residuals(modelo))),
                  alpha = 0.2, fill = "#E74C3C") +
      labs(title = paste0("Regressão Linear: ", nome_y, " ~ ", nome_x),
           subtitle = paste0(equacao, " | R² = ", round(r2, 3), " | p = ", 
                           ifelse(p_val < 0.001, "< 0.001", round(p_val, 3))),
           x = nome_x, 
           y = nome_y) +
      theme_minimal() +
      theme(
        plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
        plot.subtitle = element_text(hjust = 0.5, size = 11)
      )
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Spearman
#' @post /analise/spearman
#' @serializer json
#' Dados aceitos: Dois vetores numéricos (variáveis ordinais ou contínuas) + labels
#' Formato esperado: {"x": [1, 2, 3, 4, 5], "y": [2, 4, 5, 4, 6], "nome_x": "Var1", "nome_y": "Var2"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "spearman", exact = FALSE)
    
    return(list(
      rho = round(teste$estimate, 4),
      p_valor = round(teste$p.value, 4),
      significativo = teste$p.value < 0.05,
      estatistica_S = teste$statistic,
      interpretacao = "Correlação de ranks - apropriada para dados não-normais ou ordinais",
      nome_x = nome_x,
      nome_y = nome_y
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Spearman - Gráfico
#' @post /analise/spearman_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "spearman", exact = FALSE)
    
    df_plot <- data.frame(x = x, y = y)
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#9B59B6", alpha = 0.7) +
      geom_smooth(method = "loess", se = TRUE, color = "#E74C3C", fill = "#E74C3C", alpha = 0.2) +
      labs(title = paste0("Correlação de Spearman: ", nome_y, " vs ", nome_x),
           subtitle = paste0("ρ = ", round(teste$estimate, 3),
                           " | p = ", ifelse(teste$p.value < 0.001, "< 0.001", round(teste$p.value, 3)),
                           " | ", ifelse(teste$p.value < 0.05, "Significativo", "Não significativo")),
           x = nome_x,
           y = nome_y,
           caption = "Correlação de ranks - apropriada para dados não-normais") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Kendall
#' @post /analise/kendall
#' @serializer json
#' Dados aceitos: Dois vetores numéricos (variáveis ordinais) + labels
#' Formato esperado: {"x": [1, 2, 3, 4, 5], "y": [2, 4, 5, 4, 6], "nome_x": "Var1", "nome_y": "Var2"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "kendall", exact = FALSE)
    
    return(list(
      tau = round(teste$estimate, 4),
      p_valor = round(teste$p.value, 4),
      significativo = teste$p.value < 0.05,
      estatistica_z = teste$statistic,
      interpretacao = "Correlação de concordância - robusta a outliers",
      nome_x = nome_x,
      nome_y = nome_y
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Kendall - Gráfico
#' @post /analise/kendall_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    x <- as.numeric(dados$x)
    y <- as.numeric(dados$y)
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "Variável X"
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Variável Y"
    
    teste <- cor.test(x, y, method = "kendall", exact = FALSE)
    
    df_plot <- data.frame(x = x, y = y)
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#27AE60", alpha = 0.7) +
      geom_smooth(method = "loess", se = TRUE, color = "#E74C3C", fill = "#E74C3C", alpha = 0.2) +
      labs(title = paste0("Correlação de Kendall: ", nome_y, " vs ", nome_x),
           subtitle = paste0("τ = ", round(teste$estimate, 3),
                           " | p = ", ifelse(teste$p.value < 0.001, "< 0.001", round(teste$p.value, 3)),
                           " | ", ifelse(teste$p.value < 0.05, "Significativo", "Não significativo")),
           x = nome_x,
           y = nome_y,
           caption = "Correlação de concordância - robusta a outliers") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Kolmogorov-Smirnov
#' @post /analise/ks
#' @serializer json
#' Dados aceitos: Um ou dois vetores numéricos + labels
#' Formato esperado: {"amostra1": [1, 2, 3, 4], "amostra2": [2, 3, 4, 5], "nome_amostra1": "Local A", "nome_amostra2": "Local B"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    if (!is.null(dados$amostra2)) {
      amostra1 <- as.numeric(dados$amostra1)
      amostra2 <- as.numeric(dados$amostra2)
      nome_a1 <- if(!is.null(dados$nome_amostra1)) dados$nome_amostra1 else "Amostra 1"
      nome_a2 <- if(!is.null(dados$nome_amostra2)) dados$nome_amostra2 else "Amostra 2"
      
      teste <- ks.test(amostra1, amostra2)
      
      return(list(
        estatistica_d = round(teste$statistic, 4),
        p_valor = round(teste$p.value, 4),
        significativo = teste$p.value < 0.05,
        interpretacao = "Testa se duas amostras vêm da mesma distribuição",
        nome_amostra1 = nome_a1,
        nome_amostra2 = nome_a2
      ))
    } else {
      amostra <- as.numeric(dados$amostra)
      dist <- if(!is.null(dados$distribuicao)) dados$distribuicao else "pnorm"
      
      teste <- ks.test(amostra, dist)
      
      return(list(
        estatistica_d = round(teste$statistic, 4),
        p_valor = round(teste$p.value, 4),
        significativo = teste$p.value < 0.05,
        interpretacao = paste("Testa se a amostra segue a distribuição", dist)
      ))
    }
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Shapiro-Wilk
#' @post /analise/shapiro
#' @serializer json
#' Dados aceitos: Vetor numérico (variável contínua, máximo 5000 observações)
#' Formato esperado: {"dados": [12, 15, 14, 18, 20, 19, 22], "nome_variavel": "Riqueza"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    valores <- as.numeric(dados$dados)
    nome_var <- if(!is.null(dados$nome_variavel)) dados$nome_variavel else "Variável"
    
    teste <- shapiro.test(valores)
    
    return(list(
      estatistica_w = round(teste$statistic, 4),
      p_valor = round(teste$p.value, 4),
      normal = teste$p.value > 0.05,
      interpretacao = ifelse(teste$p.value > 0.05, 
                             "Dados seguem distribuição normal (p > 0.05)", 
                             "Dados NÃO seguem distribuição normal (p < 0.05)"),
      nome_variavel = nome_var,
      nota = "Teste sensível ao tamanho amostral - amostras grandes podem rejeitar normalidade mesmo com desvios pequenos"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Kruskal-Wallis
#' @post /analise/kruskal
#' @serializer json
#' Dados aceitos: Vetor de valores e vetor de grupos + labels
#' Formato esperado: {"valores": [12, 15, 14, 18, 20, 19], "grupos": ["A", "A", "B", "B", "C", "C"], "nome_variavel": "Abundância"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    valores <- as.numeric(dados$valores)
    grupos <- as.factor(dados$grupos)
    nome_var <- if(!is.null(dados$nome_variavel)) dados$nome_variavel else "Variável"
    
    teste <- kruskal.test(valores ~ grupos)
    
    medianas <- aggregate(valores ~ grupos, FUN = median)
    
    return(list(
      estatistica_h = round(teste$statistic, 4),
      p_valor = round(teste$p.value, 4),
      significativo = teste$p.value < 0.05,
      gl = teste$parameter,
      medianas_por_grupo = setNames(round(medianas$valores, 2), medianas$grupos),
      nome_variavel = nome_var,
      interpretacao = "Alternativa não-paramétrica à ANOVA - compara medianas entre grupos"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' RDA (Análise de Redundância)
#' @post /analise/rda
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra + lista de variáveis ambientais por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3], [1,4,2], [3,1,5], [2,3,1]],
#'   "variaveis_por_amostra": [[12.5,3.2,15.2], [15.1,2.8,18.3], [10.3,4.5,12.1], [18.2,2.1,20.5]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea"],
#'   "nomes_amostras": ["Área_1", "Área_2", "Área_3", "Área_4"],
#'   "nomes_variaveis_ambientais": ["Temperatura_°C", "pH_Solo", "Umidade_%"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_especies <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    matriz_ambiente <- criar_matriz_ambiental(
      dados$variaveis_por_amostra,
      dados$nomes_variaveis_ambientais,
      dados$nomes_amostras
    )
    
    rda_result <- rda(matriz_especies ~ ., data = as.data.frame(matriz_ambiente))
    
    var_explicada <- RsquareAdj(rda_result)
    
    site_scores <- scores(rda_result, display = "sites", choices = 1:2)
    species_scores <- scores(rda_result, display = "species", choices = 1:2)
    biplot_scores <- scores(rda_result, display = "bp", choices = 1:2)
    
    df_sites <- data.frame(
      RDA1 = site_scores[, 1],
      RDA2 = site_scores[, 2],
      Site = rownames(site_scores),
      Tipo = "Amostra"
    )
    
    df_especies <- data.frame(
      RDA1 = species_scores[, 1],
      RDA2 = species_scores[, 2],
      Site = rownames(species_scores),
      Tipo = "Espécie"
    )
    
    df_biplot <- data.frame(
      RDA1 = biplot_scores[, 1],
      RDA2 = biplot_scores[, 2],
      Variavel = rownames(biplot_scores)
    )
    
    p <- ggplot() +
      geom_point(data = df_sites, aes(x = RDA1, y = RDA2, text = paste("Amostra:", Site)),
                 size = 4, color = "#3498DB", alpha = 0.7) +
      geom_text(data = df_sites, aes(x = RDA1, y = RDA2, label = Site),
                vjust = -1, size = 3, color = "#2C3E50") +
      geom_point(data = df_especies, aes(x = RDA1, y = RDA2, text = paste("Espécie:", Site)),
                 size = 3, color = "#E74C3C", alpha = 0.7) +
      geom_text(data = df_especies, aes(x = RDA1, y = RDA2, label = Site),
                vjust = -1, size = 2.5, color = "#C0392B") +
      geom_segment(data = df_biplot, 
                   aes(x = 0, y = 0, xend = RDA1, yend = RDA2),
                   arrow = arrow(length = unit(0.3, "cm")),
                   color = "#27AE60", linewidth = 1) +
      geom_text(data = df_biplot, aes(x = RDA1, y = RDA2, label = Variavel),
                vjust = -0.5, size = 3.5, color = "#27AE60", fontface = "bold") +
      geom_hline(yintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      geom_vline(xintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      labs(title = "Análise de Redundância (RDA)",
           subtitle = paste0("R² ajustado = ", round(var_explicada$adj.r.squared, 3),
                           " | ", ncol(matriz_ambiente), " variáveis ambientais | ",
                           "Variância explicada: ", 
                           round(100 * rda_result$CCA$tot.chi / rda_result$tot.chi, 1), "%"),
           x = paste0("RDA1 (", round(100 * summary(eigenvals(rda_result, model = "constrained"))[2, 1], 1), "%)"),
           y = paste0("RDA2 (", round(100 * summary(eigenvals(rda_result, model = "constrained"))[2, 2], 1), "%)"),
           caption = "Azul = Amostras | Vermelho = Espécies | Verde = Variáveis Ambientais") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p, tooltip = "text")
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}


#' CCA (Análise de Correspondência Canônica) 
#' @post /analise/cca
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra + lista de variáveis ambientais por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3], [1,4,2], [3,1,5], [2,3,1]],
#'   "variaveis_por_amostra": [[12.5,3.2,15.2], [15.1,2.8,18.3], [10.3,4.5,12.1], [18.2,2.1,20.5]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea"],
#'   "nomes_amostras": ["Área_1", "Área_2", "Área_3", "Área_4"],
#'   "nomes_variaveis_ambientais": ["Temperatura_°C", "pH_Solo", "Umidade_%"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_especies <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    matriz_ambiente <- criar_matriz_ambiental(
      dados$variaveis_por_amostra,
      dados$nomes_variaveis_ambientais,
      dados$nomes_amostras
    )
    
    cca_result <- cca(matriz_especies ~ ., data = as.data.frame(matriz_ambiente))
    
    site_scores <- scores(cca_result, display = "sites", choices = 1:2)
    species_scores <- scores(cca_result, display = "species", choices = 1:2)
    biplot_scores <- scores(cca_result, display = "bp", choices = 1:2)
    
    total_inercia <- cca_result$tot.chi
    inercia_explicada <- cca_result$CCA$tot.chi
    
    df_sites <- data.frame(
      CCA1 = site_scores[, 1],
      CCA2 = site_scores[, 2],
      Site = rownames(site_scores),
      Tipo = "Amostra"
    )
    
    df_especies <- data.frame(
      CCA1 = species_scores[, 1],
      CCA2 = species_scores[, 2],
      Site = rownames(species_scores),
      Tipo = "Espécie"
    )
    
    df_biplot <- data.frame(
      CCA1 = biplot_scores[, 1],
      CCA2 = biplot_scores[, 2],
      Variavel = rownames(biplot_scores)
    )
    
    p <- ggplot() +
      geom_point(data = df_sites, aes(x = CCA1, y = CCA2, text = paste("Amostra:", Site)),
                 size = 4, color = "#9B59B6", alpha = 0.7) +
      geom_text(data = df_sites, aes(x = CCA1, y = CCA2, label = Site),
                vjust = -1, size = 3, color = "#6C3483") +
      geom_point(data = df_especies, aes(x = CCA1, y = CCA2, text = paste("Espécie:", Site)),
                 size = 3, color = "#E67E22", alpha = 0.7) +
      geom_text(data = df_especies, aes(x = CCA1, y = CCA2, label = Site),
                vjust = -1, size = 2.5, color = "#CA6F1E") +
      geom_segment(data = df_biplot, 
                   aes(x = 0, y = 0, xend = CCA1, yend = CCA2),
                   arrow = arrow(length = unit(0.3, "cm")),
                   color = "#16A085", linewidth = 1) +
      geom_text(data = df_biplot, aes(x = CCA1, y = CCA2, label = Variavel),
                vjust = -0.5, size = 3.5, color = "#16A085", fontface = "bold") +
      geom_hline(yintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      geom_vline(xintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      labs(title = "Análise de Correspondência Canônica (CCA)",
           subtitle = paste0(ncol(matriz_ambiente), " variáveis ambientais | ",
                           "Inércia explicada: ", 
                           round(100 * inercia_explicada / total_inercia, 1), "%"),
           x = paste0("CCA1 (", round(100 * summary(eigenvals(cca_result, model = "constrained"))[2, 1], 1), "%)"),
           y = paste0("CCA2 (", round(100 * summary(eigenvals(cca_result, model = "constrained"))[2, 2], 1), "%)"),
           caption = "Roxo = Amostras | Laranja = Espécies | Verde-azulado = Variáveis Ambientais") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p, tooltip = "text")
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' nMDS (Escalonamento Multidimensional Não-Métrico)
#' @post /analise/nmds
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    nmds_result <- metaMDS(matriz, k = 2, trymax = 100, autotransform = FALSE)
    
    site_scores <- scores(nmds_result, display = "sites")
    
    df_sites <- data.frame(
      NMDS1 = site_scores[, 1],
      NMDS2 = site_scores[, 2],
      Site = rownames(site_scores)
    )
    
    stress_quality <- ifelse(nmds_result$stress < 0.05, "Excelente (< 0.05)",
                      ifelse(nmds_result$stress < 0.1, "Boa (< 0.1)",
                      ifelse(nmds_result$stress < 0.2, "Utilizável (< 0.2)",
                      "Pobre (≥ 0.2)")))
    
    stress_color <- ifelse(nmds_result$stress < 0.05, "#27AE60",
                    ifelse(nmds_result$stress < 0.1, "#F39C12",
                    ifelse(nmds_result$stress < 0.2, "#E67E22", "#E74C3C")))
    
    p <- ggplot(df_sites, aes(x = NMDS1, y = NMDS2)) +
      geom_point(aes(text = paste("Amostra:", Site, 
                                  "<br>NMDS1:", round(NMDS1, 3),
                                  "<br>NMDS2:", round(NMDS2, 3))),
                 size = 5, color = "#3498DB", alpha = 0.7) +
      geom_text(aes(label = Site), vjust = -1.2, size = 3.5, color = "#2C3E50", fontface = "bold") +
      geom_hline(yintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      geom_vline(xintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      labs(title = "nMDS (Escalonamento Multidimensional Não-Métrico)",
           subtitle = paste0("Stress = ", round(nmds_result$stress, 3), 
                           " | Qualidade: ", stress_quality,
                           " | ", ncol(matriz), " espécies | ",
                           nrow(matriz), " amostras"),
           x = "NMDS1",
           y = "NMDS2",
           caption = paste0("Convergido: ", ifelse(nmds_result$converged, "Sim ✓", "Não ✗"),
                          " | Iterações: ", nmds_result$tries)) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10, color = stress_color))
    
    grafico <- ggplotly(p, tooltip = "text")
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' PCA (Análise de Componentes Principais) 
#' @post /analise/pca
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    pca_result <- rda(matriz)
    
    var_explicada <- summary(eigenvals(pca_result))
    
    site_scores <- scores(pca_result, display = "sites", choices = 1:2)
    species_scores <- scores(pca_result, display = "species", choices = 1:2)
    
    df_sites <- data.frame(
      PC1 = site_scores[, 1],
      PC2 = site_scores[, 2],
      Site = rownames(site_scores)
    )
    
    df_especies <- data.frame(
      PC1 = species_scores[, 1],
      PC2 = species_scores[, 2],
      Especie = rownames(species_scores)
    )
    
    p <- ggplot() +
      geom_point(data = df_sites, 
                 aes(x = PC1, y = PC2, text = paste("Amostra:", Site,
                                                    "<br>PC1:", round(PC1, 3),
                                                    "<br>PC2:", round(PC2, 3))),
                 size = 5, color = "#3498DB", alpha = 0.7) +
      geom_text(data = df_sites, aes(x = PC1, y = PC2, label = Site),
                vjust = -1.2, size = 3.5, color = "#2C3E50", fontface = "bold") +
      geom_segment(data = df_especies,
                   aes(x = 0, y = 0, xend = PC1, yend = PC2,
                       text = paste("Espécie:", Especie)),
                   arrow = arrow(length = unit(0.25, "cm")),
                   color = "#E74C3C", alpha = 0.6, linewidth = 0.8) +
      geom_text(data = df_especies, aes(x = PC1, y = PC2, label = Especie),
                vjust = -0.5, size = 2.8, color = "#C0392B", fontface = "italic") +
      geom_hline(yintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      geom_vline(xintercept = 0, linetype = "dashed", color = "gray50", alpha = 0.5) +
      labs(title = "PCA (Análise de Componentes Principais)",
           subtitle = paste0("Variância acumulada (PC1+PC2): ", 
                           round(100 * sum(var_explicada[2, 1:2]), 1), "% | ",
                           ncol(matriz), " espécies | ",
                           nrow(matriz), " amostras"),
           x = paste0("PC1 (", round(100 * var_explicada[2, 1], 1), "%)"),
           y = paste0("PC2 (", round(100 * var_explicada[2, 2], 1), "%)"),
           caption = "Azul = Amostras | Vermelho = Espécies (vetores)") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p, tooltip = "text")
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Jaccard
#' @post /analise/jaccard
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_jaccard <- vegdist(matriz_pa, method = "jaccard")
    matriz_dist <- as.matrix(dist_jaccard)
    
    return(list(
      matriz_distancia = matriz_dist,
      nomes_amostras = rownames(matriz_dist),
      numero_amostras = nrow(matriz_pa),
      numero_especies = ncol(matriz_pa),
      metodo = "Jaccard",
      interpretacao = "Valores próximos de 0 = maior similaridade | Valores próximos de 1 = menor similaridade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Jaccard (Gráfico)
#' @post /analise/jaccard_grafico
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_jaccard <- vegdist(matriz_pa, method = "jaccard")
    matriz_dist <- as.matrix(dist_jaccard)
    df_heatmap <- as.data.frame(matriz_dist) %>%
      mutate(Amostra1 = rownames(.)) %>%
      pivot_longer(cols = -Amostra1, names_to = "Amostra2", values_to = "Distancia")
    
    p <- ggplot(df_heatmap, aes(x = Amostra1, y = Amostra2, fill = Distancia)) +
      geom_tile(color = "white") +
      geom_text(aes(label = round(Distancia, 2)), color = "black", size = 3) +
      scale_fill_gradient2(low = "#27AE60", mid = "#F39C12", high = "#E74C3C",
                          midpoint = 0.5, limit = c(0, 1),
                          name = "Distância\nJaccard") +
      labs(title = "Matriz de Distância de Jaccard (Presença-Ausência)",
           x = "", y = "") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            axis.text.x = element_text(angle = 45, hjust = 1))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}


#' Bray-Curtis
#' @post /analise/bray_curtis
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_bray <- vegdist(matriz, method = "bray")
    matriz_dist <- as.matrix(dist_bray)
    
    return(list(
      matriz_distancia = matriz_dist,
      nomes_amostras = rownames(matriz_dist),
      numero_amostras = nrow(matriz),
      numero_especies = ncol(matriz),
      metodo = "Bray-Curtis",
      interpretacao = "Valores próximos de 0 = maior similaridade | Valores próximos de 1 = menor similaridade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Bray-Curtis (Gráfico)
#' @post /analise/bray_curtis_grafico
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_bray <- vegdist(matriz, method = "bray")
    matriz_dist <- as.matrix(dist_bray)
    
    df_heatmap <- as.data.frame(matriz_dist) %>%
      mutate(Amostra1 = rownames(.)) %>%
      pivot_longer(cols = -Amostra1, names_to = "Amostra2", values_to = "Distancia")
    
    p <- ggplot(df_heatmap, aes(x = Amostra1, y = Amostra2, fill = Distancia)) +
      geom_tile(color = "white") +
      geom_text(aes(label = round(Distancia, 2)), color = "black", size = 3) +
      scale_fill_gradient2(low = "#3498DB", mid = "#F39C12", high = "#E74C3C",
                          midpoint = 0.5, limit = c(0, 1),
                          name = "Distância\nBray-Curtis") +
      labs(title = "Matriz de Dissimilaridade de Bray-Curtis (Abundância)",
           x = "", y = "") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            axis.text.x = element_text(angle = 45, hjust = 1))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Morisita-Horn
#' @post /analise/morisita
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_morisita <- vegdist(matriz, method = "horn")
    matriz_dist <- as.matrix(dist_morisita)
    
    return(list(
      matriz_distancia = matriz_dist,
      nomes_amostras = rownames(matriz_dist),
      numero_amostras = nrow(matriz),
      numero_especies = ncol(matriz),
      metodo = "Morisita-Horn",
      interpretacao = "Valores próximos de 0 = maior similaridade | Valores próximos de 1 = menor similaridade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Morisita-Horn (Gráfico)
#' @post /analise/morisita_grafico
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[5,2,3,1], [1,4,2,0], [3,1,5,2], [2,3,1,4]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata"],
#'   "nomes_amostras": ["Local_Norte", "Local_Sul", "Local_Leste", "Local_Oeste"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz <- criar_matriz_abundancia(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_morisita <- vegdist(matriz, method = "horn")
    matriz_dist <- as.matrix(dist_morisita)
    
    df_heatmap <- as.data.frame(matriz_dist) %>%
      mutate(Amostra1 = rownames(.)) %>%
      pivot_longer(cols = -Amostra1, names_to = "Amostra2", values_to = "Distancia")
    
    p <- ggplot(df_heatmap, aes(x = Amostra1, y = Amostra2, fill = Distancia)) +
      geom_tile(color = "white") +
      geom_text(aes(label = round(Distancia, 2)), color = "black", size = 3) +
      scale_fill_gradient2(low = "#9B59B6", mid = "#F39C12", high = "#E74C3C",
                          midpoint = 0.5, limit = c(0, 1),
                          name = "Distância\nMorisita-Horn") +
      labs(title = "Matriz de Dissimilaridade de Morisita-Horn (Abundância)",
           x = "", y = "") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            axis.text.x = element_text(angle = 45, hjust = 1))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Sørensen-Dice
#' @post /analise/sorensen
#' @serializer json
#' Dados aceitos: Lista de abundâncias por amostra
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_sorensen <- vegdist(matriz_pa, method = "bray", binary = TRUE)
    matriz_dist <- as.matrix(dist_sorensen)
    
    return(list(
      matriz_distancia = matriz_dist,
      nomes_amostras = rownames(matriz_dist),
      numero_amostras = nrow(matriz_pa),
      numero_especies = ncol(matriz_pa),
      metodo = "Sørensen-Dice",
      interpretacao = "Valores próximos de 0 = maior similaridade | Valores próximos de 1 = menor similaridade"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Sørensen-Dice (Gráfico)
#' @post /analise/sorensen_grafico
#' @serializer html
#' Dados aceitos: Lista de abundâncias por amostra 
#' Formato esperado: {
#'   "abundancias_por_amostra": [[1,0,1,0,1], [1,1,0,0,1], [0,1,1,1,0], [1,0,1,1,1]],
#'   "nomes_especies": ["Apis mellifera", "Bombus terrestris", "Xylocopa violacea", "Megachile rotundata", "Osmia bicornis"],
#'   "nomes_amostras": ["Unidade_A1", "Unidade_A2", "Unidade_B1", "Unidade_B2"]
#' }
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    
    matriz_pa <- criar_matriz_pa(
      dados$abundancias_por_amostra,
      dados$nomes_especies,
      dados$nomes_amostras
    )
    
    dist_sorensen <- vegdist(matriz_pa, method = "bray", binary = TRUE)
    matriz_dist <- as.matrix(dist_sorensen)
    
    df_heatmap <- as.data.frame(matriz_dist) %>%
      mutate(Amostra1 = rownames(.)) %>%
      pivot_longer(cols = -Amostra1, names_to = "Amostra2", values_to = "Distancia")
    
    p <- ggplot(df_heatmap, aes(x = Amostra1, y = Amostra2, fill = Distancia)) +
      geom_tile(color = "white") +
      geom_text(aes(label = round(Distancia, 2)), color = "black", size = 3) +
      scale_fill_gradient2(low = "#27AE60", mid = "#F39C12", high = "#E74C3C",
                          midpoint = 0.5, limit = c(0, 1),
                          name = "Distância\nSørensen") +
      labs(title = "Matriz de Distância de Sørensen-Dice (Presença-Ausência)",
           x = "", y = "") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            axis.text.x = element_text(angle = 45, hjust = 1))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}


#' Brillouin
#' @post /analise/brillouin
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    N <- sum(abundancias)
    HB <- (lfactorial(N) - sum(lfactorial(abundancias))) / N
    
    return(list(
      indice_brillouin = round(HB, 4),
      total_individuos = N,
      interpretacao = "Apropriado para comunidades completamente amostradas - mais conservador que Shannon"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' MacIntosh
#' @post /analise/macintosh
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    N <- sum(abundancias)
    U <- sqrt(sum(abundancias^2))
    D <- (N - U) / (N - sqrt(N))
    
    return(list(
      indice_macintosh = round(D, 4),
      distancia_U = round(U, 4),
      total_individuos = N,
      interpretacao = "Medida de diversidade baseada em distância euclidiana - varia de 0 a 1"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Hurlbert's PIE
#' @post /analise/hurlbert
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie)
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    
    N <- sum(abundancias)
    pie <- sum((abundancias/N) * ((N - abundancias)/(N - 1)))
    
    return(list(
      hurlbert_pie = round(pie, 4),
      total_individuos = N,
      interpretacao = "Probabilidade de que dois indivíduos selecionados ao acaso sejam de espécies diferentes"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Índice de Dominância de McNaughton
#' @post /analise/mcnaughton
#' @serializer json
#' Dados aceitos: Vetor de abundâncias (quantidade de indivíduos por espécie) + nomes opcionais
#' Formato esperado: {"abundancias": [5, 12, 3, 45, 2, 8], "nomes_especies": ["Sp1", "Sp2", ...]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    abundancias <- as.numeric(dados$abundancias)
    nomes_especies <- if(!is.null(dados$nomes_especies)) dados$nomes_especies else paste0("Sp", 1:length(abundancias))
    df_ordenado <- data.frame(
      especie = nomes_especies,
      abundancia = abundancias
    ) %>% arrange(desc(abundancia))
    
    N <- sum(abundancias)
    mcn <- (df_ordenado$abundancia[1] + df_ordenado$abundancia[2]) / N
    
    return(list(
      indice_mcnaughton = round(mcn, 4),
      proporcao_duas_especies_dominantes = paste0(round(mcn * 100, 2), "%"),
      especie_mais_abundante = df_ordenado$especie[1],
      abundancia_primeira = df_ordenado$abundancia[1],
      segunda_mais_abundante = df_ordenado$especie[2],
      abundancia_segunda = df_ordenado$abundancia[2],
      interpretacao = "Proporção representada pelas duas espécies mais abundantes - valores altos indicam forte dominância"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Gaussiano (Normal)
#' @post /analise/modelo_gaussiano
#' @serializer json
#' Dados aceitos: Vetor de resposta (contínua) e preditoras + labels
#' Formato esperado: {"y": [2.1, 3.5, 4.2], "x": [1, 2, 3], "nome_y": "Riqueza", "nome_x": "Temperatura"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.numeric(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = gaussian())
    
    return(list(
      aic = round(AIC(modelo), 2),
      deviance = round(deviance(modelo), 4),
      coeficientes = round(coef(modelo), 4),
      r_quadrado = round(1 - (deviance(modelo) / sum((y - mean(y))^2)), 4),
      nome_resposta = nome_y,
      nome_preditor = nome_x,
      familia = "Gaussiana (Normal)",
      interpretacao = "Apropriado para variáveis resposta contínuas com distribuição normal"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Gráfico do Modelo Gaussiano
#' @post /analise/modelo_gaussiano_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.numeric(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = gaussian())
    
    df_plot <- data.frame(
      x = x,
      y = y,
      fitted = fitted(modelo),
      residuals = residuals(modelo)
    )
    
    r2 <- 1 - (deviance(modelo) / sum((y - mean(y))^2))
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#3498DB", alpha = 0.7) +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      geom_ribbon(aes(ymin = fitted - sd(residuals), 
                      ymax = fitted + sd(residuals)),
                  alpha = 0.2, fill = "#E74C3C") +
      labs(title = paste0("Modelo Gaussiano (GLM): ", nome_y, " ~ ", nome_x),
           subtitle = paste0("AIC = ", round(AIC(modelo), 2), 
                           " | R² = ", round(r2, 3),
                           " | Deviance = ", round(deviance(modelo), 2)),
           x = nome_x,
           y = nome_y,
           caption = "Linha vermelha = valores ajustados | Banda = ± 1 desvio padrão") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Gamma
#' @post /analise/modelo_gamma
#' @serializer json
#' Dados aceitos: Vetor de resposta (positiva contínua) e preditoras
#' Formato esperado: {"y": [2.1, 3.5, 4.2], "x": [1, 2, 3], "nome_y": "Biomassa", "nome_x": "Área"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.numeric(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = Gamma(link = "log"))
    
    return(list(
      aic = round(AIC(modelo), 2),
      deviance = round(deviance(modelo), 4),
      coeficientes = round(coef(modelo), 4),
      nome_resposta = nome_y,
      nome_preditor = nome_x,
      familia = "Gamma",
      interpretacao = "Apropriado para dados contínuos positivos com variância proporcional à média"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Gráfico do Modelo Gamma
#' @post /analise/modelo_gamma_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.numeric(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = Gamma(link = "log"))
    
    df_plot <- data.frame(
      x = x,
      y = y,
      fitted = fitted(modelo)
    )
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#9B59B6", alpha = 0.7) +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      labs(title = paste0("Modelo Gamma (GLM): ", nome_y, " ~ ", nome_x),
           subtitle = paste0("AIC = ", round(AIC(modelo), 2), 
                           " | Deviance = ", round(deviance(modelo), 2),
                           " | Link = log"),
           x = nome_x,
           y = nome_y,
           caption = "Apropriado para dados positivos contínuos") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Poisson
#' @post /analise/modelo_poisson
#' @serializer json
#' Dados aceitos: Vetor de contagens e preditoras
#' Formato esperado: {"y": [5, 12, 8, 3], "x": [1, 2, 3, 4], "nome_y": "Número de espécies", "nome_x": "Área"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.integer(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = poisson())
    
    return(list(
      aic = round(AIC(modelo), 2),
      deviance = round(deviance(modelo), 4),
      coeficientes = round(coef(modelo), 4),
      sobredispersao = round(deviance(modelo) / df.residual(modelo), 4),
      nome_resposta = nome_y,
      nome_preditor = nome_x,
      familia = "Poisson",
      interpretacao = "Apropriado para dados de contagem. Se sobredispersão > 1.5, considere Binomial Negativa",
      nota_sobredispersao = ifelse(deviance(modelo) / df.residual(modelo) > 1.5,
                                   "ATENÇÃO: Sobredispersão detectada - considere modelo Binomial Negativa",
                                   "Sem sobredispersão significativa")
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Gráfico do Modelo Poisson
#' @post /analise/modelo_poisson_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.integer(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm(y ~ x, family = poisson())
    sobredispersao <- deviance(modelo) / df.residual(modelo)
    
    df_plot <- data.frame(
      x = x,
      y = y,
      fitted = fitted(modelo)
    )
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#27AE60", alpha = 0.7) +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      labs(title = paste0("Modelo Poisson (GLM): ", nome_y, " ~ ", nome_x),
           subtitle = paste0("AIC = ", round(AIC(modelo), 2), 
                           " | Sobredispersão = ", round(sobredispersao, 2),
                           ifelse(sobredispersao > 1.5, "ALTA", "OK")),
           x = nome_x,
           y = nome_y,
           caption = ifelse(sobredispersao > 1.5, 
                          "Sobredispersão detectada - considere Binomial Negativa",
                          "Modelo adequado para os dados")) +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Modelo Binomial Negativa
#' @post /analise/modelo_binomial_negativa
#' @serializer json
#' Dados aceitos: Vetor de contagens e preditoras
#' Formato esperado: {"y": [5, 12, 8, 3], "x": [1, 2, 3, 4], "nome_y": "Abundância", "nome_x": "Precipitação"}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.integer(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm.nb(y ~ x)
    
    return(list(
      aic = round(AIC(modelo), 2),
      deviance = round(deviance(modelo), 4),
      coeficientes = round(coef(modelo), 4),
      theta = round(modelo$theta, 4),
      nome_resposta = nome_y,
      nome_preditor = nome_x,
      familia = "Binomial Negativa",
      interpretacao = "Apropriado para dados de contagem com sobredispersão (variância > média)"
    ))
  }, error = function(e) {
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Gráfico do Modelo Binomial Negativa
#' @post /analise/modelo_binomial_grafico
#' @serializer html
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    y <- as.integer(dados$y)
    x <- as.numeric(dados$x)
    nome_y <- if(!is.null(dados$nome_y)) dados$nome_y else "Y"
    nome_x <- if(!is.null(dados$nome_x)) dados$nome_x else "X"
    
    modelo <- glm.nb(y ~ x)
    
    df_plot <- data.frame(
      x = x,
      y = y,
      fitted = fitted(modelo)
    )
    
    p <- ggplot(df_plot, aes(x = x, y = y)) +
      geom_point(size = 3, color = "#E67E22", alpha = 0.7) +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      labs(title = paste0("Modelo Binomial Negativa (GLM): ", nome_y, " ~ ", nome_x),
           subtitle = paste0("AIC = ", round(AIC(modelo), 2), 
                           " | θ = ", round(modelo$theta, 2),
                           " | Deviance = ", round(deviance(modelo), 2)),
           x = nome_x,
           y = nome_y,
           caption = "Apropriado para dados de contagem com sobredispersão") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5, size = 10))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}

#' Michaelis-Menten
#' @post /analise/michaelis_menten
#' @serializer html
#' Dados aceitos: Matriz de espécies por amostras (amostras em ordem de coleta)
#' Formato esperado: {"matriz": [[5,2,3], [1,4,2], [3,1,5]]}
function(req, res) {
  tryCatch({
    dados <- fromJSON(req$postBody)
    matriz <- as.matrix(dados$matriz)
    
    acum <- specaccum(matriz, method = "collector")
    
    fit <- nls(richness ~ Smax * sites / (k + sites), 
               data = data.frame(richness = acum$richness, sites = acum$sites),
               start = list(Smax = max(acum$richness) * 1.5, k = median(acum$sites)))
    
    Smax <- coef(fit)["Smax"]
    k <- coef(fit)["k"]
    
    df_plot <- data.frame(
      sites = acum$sites,
      richness = acum$richness,
      fitted = predict(fit)
    )
    
    p <- ggplot(df_plot, aes(x = sites)) +
      geom_point(aes(y = richness), size = 3, color = "#3498DB") +
      geom_line(aes(y = fitted), color = "#E74C3C", linewidth = 1.2) +
      geom_hline(yintercept = Smax, linetype = "dashed", color = "gray50") +
      annotate("text", x = max(acum$sites) * 0.7, y = Smax + 1,
               label = paste0("Riqueza máxima estimada: ", round(Smax, 1)), 
               color = "gray50") +
      labs(title = "Curva de Acumulação de Espécies (Michaelis-Menten)",
           subtitle = paste0("Smax = ", round(Smax, 2), " | k = ", round(k, 2)),
           x = "Número de Amostras", 
           y = "Riqueza Acumulada",
           caption = "Pontos azuis = dados observados | Linha vermelha = modelo ajustado") +
      theme_minimal() +
      theme(plot.title = element_text(hjust = 0.5, face = "bold", size = 14),
            plot.subtitle = element_text(hjust = 0.5))
    
    grafico <- ggplotly(p)
    html <- salvar_grafico(grafico)

    if (!is.null(html)) {
      return(html)
    } else {
      stop("Falha na geração do HTML.")
    }
  }, error = function(e) {
    cat("Erro:", conditionMessage(e), "\n")
    res$status <- 500
    return(list(error = paste("Erro:", e$message)))
  })
}