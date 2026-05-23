export type ChaveAnalise =
  | "lognormal"
  | "logserie"
  | "geometrica"
  | "vara_quebrada"
  | "rarefacao"
  | "jackknife1"
  | "jackknife2"
  | "chao1"
  | "chao2"
  | "bootstrap"
  | "ace"
  | "ice"
  | "shannon"
  | "simpson"
  | "margalef"
  | "pielou"
  | "berger_parker"
  | "brillouin"
  | "macintosh"
  | "hurlbert"
  | "mcnaughton"
  | "teste_t"
  | "ks"
  | "shapiro"
  | "anova"
  | "kruskal"
  | "pearson"
  | "spearman"
  | "kendall"
  | "regressao_linear"
  | "jaccard"
  | "bray_curtis"
  | "morisita"
  | "sorensen"
  | "rda"
  | "cca"
  | "nmds"
  | "pca"
  | "modelo_gaussiano"
  | "modelo_gamma"
  | "modelo_poisson"
  | "modelo_binomial_negativa"
  | "michaelis_menten";

export type CategoriaAnalise =
  | "modelo_distribuicao"
  | "rarefacao"
  | "estimador_riqueza"
  | "indice_diversidade"
  | "teste_hipotese"
  | "correlacao"
  | "regressao"
  | "similaridade"
  | "multivariada"
  | "glm"
  | "acumulacao";

export type TipoDadoAnalise =
  | "abundancias"
  | "abundancias_por_amostra"
  | "abundancias_com_variaveis"
  | "matriz_acumulacao"
  | "dois_vetores"
  | "vetor_unico"
  | "dois_grupos"
  | "multiplos_grupos";

export type FonteAnalise = "variavel" | "abundancia" | "riqueza";
export type NivelAgregacao = "campanha" | "unidade_amostral" | "evento";
export type AgruparPor =
  | "campanha"
  | "unidade_amostral"
  | "evento"
  | "mes"
  | "ano"
  | "estacao";

export interface CatalogoAnalise {
  chave: ChaveAnalise;
  nome: string;
  categoria: CategoriaAnalise;
  tipo_dado: TipoDadoAnalise;
  tem_valor: boolean;
  tem_grafico: boolean;
}

export const NOME_CATEGORIA: Record<CategoriaAnalise, string> = {
  modelo_distribuicao: "Modelo de Distribuição",
  rarefacao: "Rarefação",
  estimador_riqueza: "Estimador de Riqueza",
  indice_diversidade: "Índice de Diversidade",
  teste_hipotese: "Teste de Hipótese",
  correlacao: "Correlação",
  regressao: "Regressão",
  similaridade: "Similaridade",
  multivariada: "Multivariada",
  glm: "GLM",
  acumulacao: "Acumulação de Espécies",
};

export const CATALOGO_ANALISES: readonly CatalogoAnalise[] = [
  {
    chave: "lognormal",
    nome: "Log-Normal",
    categoria: "modelo_distribuicao",
    tipo_dado: "abundancias",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "logserie",
    nome: "Log-Série",
    categoria: "modelo_distribuicao",
    tipo_dado: "abundancias",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "geometrica",
    nome: "Geométrica",
    categoria: "modelo_distribuicao",
    tipo_dado: "abundancias",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "vara_quebrada",
    nome: "Vara Quebrada",
    categoria: "modelo_distribuicao",
    tipo_dado: "abundancias",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "rarefacao",
    nome: "Rarefação",
    categoria: "rarefacao",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "jackknife1",
    nome: "Jackknife 1ª Ordem",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "jackknife2",
    nome: "Jackknife 2ª Ordem",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "chao1",
    nome: "Chao1",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "chao2",
    nome: "Chao2",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "bootstrap",
    nome: "Bootstrap",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "ace",
    nome: "ACE",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "ice",
    nome: "ICE",
    categoria: "estimador_riqueza",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "shannon",
    nome: "Shannon-Wiener",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "simpson",
    nome: "Simpson",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "margalef",
    nome: "Margalef",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "pielou",
    nome: "Pielou",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "berger_parker",
    nome: "Berger-Parker",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "brillouin",
    nome: "Brillouin",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "macintosh",
    nome: "McIntosh",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "hurlbert",
    nome: "Hurlbert",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "mcnaughton",
    nome: "McNaughton",
    categoria: "indice_diversidade",
    tipo_dado: "abundancias",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "teste_t",
    nome: "Teste T",
    categoria: "teste_hipotese",
    tipo_dado: "dois_grupos",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "ks",
    nome: "Kolmogorov-Smirnov",
    categoria: "teste_hipotese",
    tipo_dado: "dois_grupos",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "shapiro",
    nome: "Shapiro-Wilk",
    categoria: "teste_hipotese",
    tipo_dado: "vetor_unico",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "anova",
    nome: "ANOVA",
    categoria: "teste_hipotese",
    tipo_dado: "multiplos_grupos",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "kruskal",
    nome: "Kruskal-Wallis",
    categoria: "teste_hipotese",
    tipo_dado: "multiplos_grupos",
    tem_valor: true,
    tem_grafico: false,
  },
  {
    chave: "pearson",
    nome: "Correlação de Pearson",
    categoria: "correlacao",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "spearman",
    nome: "Correlação de Spearman",
    categoria: "correlacao",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "kendall",
    nome: "Correlação de Kendall",
    categoria: "correlacao",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "regressao_linear",
    nome: "Regressão Linear",
    categoria: "regressao",
    tipo_dado: "dois_vetores",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "jaccard",
    nome: "Índice de Jaccard",
    categoria: "similaridade",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "bray_curtis",
    nome: "Bray-Curtis",
    categoria: "similaridade",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "morisita",
    nome: "Morisita",
    categoria: "similaridade",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "sorensen",
    nome: "Sørensen",
    categoria: "similaridade",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "rda",
    nome: "RDA",
    categoria: "multivariada",
    tipo_dado: "abundancias_com_variaveis",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "cca",
    nome: "CCA",
    categoria: "multivariada",
    tipo_dado: "abundancias_com_variaveis",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "nmds",
    nome: "nMDS",
    categoria: "multivariada",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "pca",
    nome: "PCA",
    categoria: "multivariada",
    tipo_dado: "abundancias_por_amostra",
    tem_valor: false,
    tem_grafico: true,
  },
  {
    chave: "modelo_gaussiano",
    nome: "GLM Gaussiano",
    categoria: "glm",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "modelo_gamma",
    nome: "GLM Gamma",
    categoria: "glm",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "modelo_poisson",
    nome: "GLM Poisson",
    categoria: "glm",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "modelo_binomial_negativa",
    nome: "GLM Binomial Negativa",
    categoria: "glm",
    tipo_dado: "dois_vetores",
    tem_valor: true,
    tem_grafico: true,
  },
  {
    chave: "michaelis_menten",
    nome: "Michaelis-Menten",
    categoria: "acumulacao",
    tipo_dado: "matriz_acumulacao",
    tem_valor: false,
    tem_grafico: true,
  },
];

export interface ExecutarAnaliseRequest {
  chave: ChaveAnalise;
  variavel_ids?: number[];
  variavel_x_id?: number;
  variavel_y_id?: number;
  fonte_x?: FonteAnalise;
  fonte_y?: FonteAnalise;
  variavel_id?: number;
  fonte?: FonteAnalise;
  nivel_agregacao?: NivelAgregacao;
  grupo1_ids?: number[];
  grupo2_ids?: number[];
  nome_grupo1?: string;
  nome_grupo2?: string;
  agrupar_por?: AgruparPor;
  campanha_ids?: number[];
  unidade_ids?: number[];
  evento_ids?: number[];
  data_inicio?: string;
  data_fim?: string;
  latitude_min?: number;
  latitude_max?: number;
  longitude_min?: number;
  longitude_max?: number;
}

export interface ExecutarAnaliseResponse {
  analise: string;
  nome: string;
  valor: Record<string, unknown> | string | null;
  grafico: string | null;
  urlArquivo: string | null;
  aviso?: string;
}
