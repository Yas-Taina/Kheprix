export interface UnidadeAmostral {
  id: number;
  campanha_id: number;
  nome: string;
  latitude: number;
  longitude: number;
  raio: number;
  metodo_coleta: string;
  esforco_amostral: string;
  created_at: string;
  updated_at: string;
}

export interface CriarUnidadeAmostralPayload {
  nome: string;
  latitude: number;
  longitude: number;
  raio?: number;
  metodo_coleta?: string;
  esforco_amostral?: string;
}

export interface AtualizarUnidadeAmostralPayload {
  nome: string;
  latitude: number;
  longitude: number;
  raio?: number;
  metodo_coleta?: string;
  esforco_amostral?: string;
}
