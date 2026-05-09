export type StatusConservacao =
  | "LC"
  | "NT"
  | "VU"
  | "EN"
  | "CR"
  | "EW"
  | "EX"
  | "DD";

export const StatusConservacaoLabels: Record<StatusConservacao, string> = {
  LC: "Menos Preocupante",
  NT: "Quase Ameaçada",
  VU: "Vulnerável",
  EN: "Em Perigo",
  CR: "Criticamente em Perigo",
  EW: "Extinto na Natureza",
  EX: "Extinto",
  DD: "Dados Insuficientes",
};

export interface Especie {
  id: number;
  estudo_id: number;
  foto: string;
  classe: string;
  ordem: string;
  familia: string;
  genero: string;
  especie: string;
  nome_popular: string;
  status_conservacao: StatusConservacao;
  endemismo: boolean;
  created_at: string;
}

export interface EspecieCreate {
  classe: string;
  ordem: string;
  familia: string;
  genero: string;
  especie: string;
  endemismo: boolean;
  foto?: string;
  nome_popular?: string;
  status_conservacao?: StatusConservacao;
}

export interface EspecieUpdate {
  foto?: string;
  classe?: string;
  ordem?: string;
  familia?: string;
  genero?: string;
  especie?: string;
  nome_popular?: string;
  status_conservacao?: StatusConservacao;
  endemismo?: boolean;
}
