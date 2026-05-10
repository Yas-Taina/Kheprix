export type StatusConvite = "pendente" | "aceito" | "recusado" | "expirado";

export interface Convite {
  id: number;
  estudo_id: number;
  email_convidado: string;
  token: string;
  status: StatusConvite;
  data_expiracao: string;
  created_at: string;
}

export interface ConviteRecebido {
  id: number;
  estudo_id: number;
  nome_estudo: string;
  nome_remetente?: string;
  token: string;
  status: StatusConvite;
  data_expiracao: string;
  created_at: string;
}

export interface ConvitePublico {
  id: number;
  estudo_id: number;
  nome_estudo: string;
  email_convidado: string;
  status: StatusConvite;
  data_expiracao: string;
}
