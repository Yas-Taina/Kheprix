-- Gold: Dimensão Tempo
-- Estações calculadas para hemisfério sul (verão=dez-fev, inverno=jun-ago)
SELECT DISTINCT
  CAST(TO_CHAR(data, 'YYYYMMDD') AS INTEGER) AS id_data,
  data AS data_completa,
  EXTRACT(DAY FROM data)::integer AS dia,
  EXTRACT(MONTH FROM data)::integer AS mes,
  EXTRACT(YEAR FROM data)::integer AS ano,
  EXTRACT(QUARTER FROM data)::integer AS trimestre,
  CASE
    WHEN EXTRACT(MONTH FROM data) IN (12,1,2) THEN 'Verão'
    WHEN EXTRACT(MONTH FROM data) IN (3,4,5) THEN 'Outono'
    WHEN EXTRACT(MONTH FROM data) IN (6,7,8) THEN 'Inverno'
    WHEN EXTRACT(MONTH FROM data) IN (9,10,11) THEN 'Primavera'
  END AS estacao
FROM public.silver_registro_ocorrencias