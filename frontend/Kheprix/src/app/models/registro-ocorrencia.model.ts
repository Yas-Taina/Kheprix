import { ValorVariavel } from "./variavel.model";

export interface RegistroOcorrencia {
  id: number;
  evento_amostragem_id: number;
  especie_id: number;
  data: string;
  hora: string;
  latitude: number;
  longitude: number;
  qtde_individuos: number;
  foto: string;
  ausencia_especie: boolean;
  created_at: string;
  valores_variaveis?: ValorVariavel[];
}

export interface RegistroOcorrenciaCreate {
  especie_id: number;
  data: string;
  hora: string;
  latitude: number;
  longitude: number;
  qtde_individuos?: number;
  foto?: string;
  ausencia_especie?: boolean;
  valores_variaveis?: ValorVariavel[];
}

export interface RegistroOcorrenciaUpdate {
  especie_id?: number;
  data?: string;
  hora?: string;
  latitude?: number;
  longitude?: number;
  qtde_individuos?: number;
  foto?: string;
  ausencia_especie?: boolean;
  valores_variaveis?: ValorVariavel[];
}
