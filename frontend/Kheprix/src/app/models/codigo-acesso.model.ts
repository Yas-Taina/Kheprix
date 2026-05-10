import { PerfilColaborador } from "./colaborador.model";

export interface CodigoAcesso {
  codigo: string;
  senha_autocadastro: string;
}

export interface AutocadastroEstudoRequest {
  codigo: string;
  senha_autocadastro: string;
}

export interface AutocadastroEstudoResponse {
  estudo_id: number;
  nome_estudo: string;
  perfil: PerfilColaborador;
}
