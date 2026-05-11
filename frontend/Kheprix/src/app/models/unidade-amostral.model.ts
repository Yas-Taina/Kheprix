import { ValorVariavel } from "./variavel.model";

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
  valores_variaveis?: ValorVariavel[];
}

export interface UnidadeAmostralCreate {
  nome: string;
  latitude: number;
  longitude: number;
  raio?: number;
  metodo_coleta?: string;
  esforco_amostral?: string;
  valores_variaveis?: ValorVariavel[];
}

export interface UnidadeAmostralUpdate extends UnidadeAmostralCreate {}
