-- Gold: Dimensão Registro de Ocorrência
-- ausencia_especie=true → quantidade_apurada=0 (registro válido para análise de distribuição)
SELECT
  r.id_registro,
  r.especie_id,
  r.evento_amostragem_id,
  r.data,
  r.latitude,
  r.longitude,
  r.data_atualizacao,
  CASE
    WHEN r.ausencia_especie THEN 0
    ELSE COALESCE(r.qtde_individuos, 1)
  END AS quantidade_apurada
FROM public.silver_registro_ocorrencias r