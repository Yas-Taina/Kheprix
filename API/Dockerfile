FROM rocker/r-ver:4.4.1

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

RUN install2.r --error --skipinstalled \
    plumber \
    vegan \
    ggplot2 \
    plotly \
    htmlwidgets \
    jsonlite \
    dplyr \
    tidyr \
    MASS

WORKDIR /app
COPY plumber.R inicializacao.R ./

EXPOSE 8000

CMD ["Rscript", "inicializacao.R"]
