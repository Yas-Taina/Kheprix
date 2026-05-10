export interface Dashboard {
  id: number;
  nome: string;
  updated_at: string;
  data_inicio: string;
  total_registros: number;
  total_especies: number;
  especies_ameacadas: number;
  especies_nativas: number;
  especies_invasoras: number;
  total_individuos: number;
}

export interface EstudoDashboard {
  estudo: {
    id: number;
    nome: string;
    updated_at: string;
  };
  resumo: EstudoDashboardResumo;
  registros_por_data: RegistroPorData[];
  ocorrencias_por_especie: OcorrenciaPorEspecie[];
  pontos_mapa: PontoMapa[];
  registros_por_especie_tempo: RegistroEspecieTempo[];
  especies_distintas_por_mes: EspecieDistintaMes[];
}

export interface EstudoDashboardResumo {
  total_registros: number;
  total_especies: number;
  especies_ameacadas: number;
  especies_nativas: number;
  especies_invasoras: number;
  total_individuos: number;
  data_inicio: string;
}

export interface RegistroPorData {
  data: string;
  total: number;
}

export interface OcorrenciaPorEspecie {
  nome_cientifico: string;
  nome_popular: string;
  total: number;
}

export interface PontoMapa {
  latitude: number;
  longitude: number;
  nome_cientifico: string;
  quantidade: number;
}

export interface RegistroEspecieTempo {
  ano: number;
  mes: number;
  nome_cientifico: string;
  is_endemica: boolean;
  total: number;
}

export interface EspecieDistintaMes {
  ano: number;
  mes: number;
  total: number;
}

export type AgrupamentoTempo = "mes" | "ano";
