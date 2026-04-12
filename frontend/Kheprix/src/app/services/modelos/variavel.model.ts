export interface Variavel {
  id: number;
  nome: string;
  metrica: string;
  nivel_aplicacao: string;
  tipo_dado: string;
  created_at: string;
  updated_at: string;
}

export type NivelAplicacao = 'campanha' | 'unidade' | 'evento' | 'registro';
