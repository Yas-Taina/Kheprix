export interface UnidadeAmostral {
  id: number;
  campanha_id: number;
  latitude: number;
  longitude: number;
  raio: number;
  metodo_coleta: string;
  esforco_amostral: string;
  created_at: string;
  updated_at: string;
}

export interface CriarUnidadeAmostralRequest {
  latitude: number;
  longitude: number;
  raio?: number;
  metodo_coleta?: string;
  esforco_amostral?: string;
}

export interface AtualizarUnidadeAmostralRequest {
  latitude: number;
  longitude: number;
  raio?: number;
  metodo_coleta?: string;
  esforco_amostral?: string;
}
