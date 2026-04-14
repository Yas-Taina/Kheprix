export type PerfilColaborador = 'proprietario' | 'co-proprietario' | 'colaborador';

export interface Colaborador {
  id_usuario: number;
  nome: string;
  email: string;
  perfil: PerfilColaborador;
}

export interface ColaboradorUpdate {
  perfil: PerfilColaborador;
}
