export type NivelAplicacao = 'campanha' | 'unidade' | 'evento' | 'registro';
export type TipoDado = 'numerico' | 'texto' | 'logico';

export interface VariavelCreate {
  nome: string;
  nivel_aplicacao: NivelAplicacao;
  tipo_dado: TipoDado;
  metrica?: string;
}

export interface Variavel {
  id: number;
  nome: string;
  metrica: string;
  nivel_aplicacao: NivelAplicacao;
  tipo_dado: TipoDado;
  created_at: string;
  updated_at: string;
}

export interface ValorVariavel {
  variavel_id: number;
  valor: string;
}
