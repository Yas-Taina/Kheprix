export type TipoMensagem = 'usuario' | 'bot' | 'sistema' | 'insights' | 'carregando';

export interface DadosTabela {
  colunas: string[];
  linhas: Record<string, unknown>[];
}

export interface InsightsMetricas {
  resumo: Record<string, unknown>[];
  top_especies: Record<string, unknown>[];
  conservacao: Record<string, unknown>[];
  sazonalidade: Record<string, unknown>[];
  taxonomia: Record<string, unknown>[];
}

export interface MensagemChat {
  tipo: TipoMensagem;
  conteudo: string;
  tabela?: DadosTabela;
  sql?: string;
  metricas?: InsightsMetricas;
  mostrarSql?: boolean;
  mostrarMetricas?: boolean;
}

export interface EstudoOpcao {
  id: number;
  nome: string;
  selecionado: boolean;
}

export interface QueryResponse {
  resposta: string | null;
  dados: Record<string, unknown>[];
  sql: string | null;
  total: number;
  erro: string | null;
}

export interface InsightsResponse {
  narrativa: string;
  metricas: InsightsMetricas;
  erro: string | null;
}
