export type NivelAplicacao = "campanha" | "unidade" | "evento" | "registro";
export type TipoDado = "numerico" | "texto" | "logico";

export const NivelLabels: Record<NivelAplicacao, string> = {
  campanha: "Campanha de Coleta",
  unidade: "Unidade Amostral",
  evento: "Evento de Amostragem",
  registro: "Registro de Ocorrência",
};

export const TipoLabels: Record<TipoDado, string> = {
  numerico: "Numérico",
  texto: "Texto",
  logico: "Lógico",
};

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
