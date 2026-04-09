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
  status_conservacao: string;
  endemismo: boolean;
  created_at: string;
}

export interface CriarEspeciePayload {
  classe: string;
  ordem: string;
  familia: string;
  genero: string;
  especie: string;
  endemismo: boolean;
  foto?: string;
  nome_popular?: string;
  status_conservacao?: string;
}

export interface AtualizarEspeciePayload {
  foto?: string;
  classe?: string;
  ordem?: string;
  familia?: string;
  genero?: string;
  especie?: string;
  nome_popular?: string;
  status_conservacao?: string;
  endemismo?: boolean;
}
