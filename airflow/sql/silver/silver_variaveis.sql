SELECT
  id,
  nome,
  metrica,
  CASE
    WHEN nivel_aplicacao = 0 THEN 'campanha'
    WHEN nivel_aplicacao = 1 THEN 'unidade'
    WHEN nivel_aplicacao = 2 THEN 'evento'
    WHEN nivel_aplicacao = 3 THEN 'registro'
    ELSE 'unknown'
  END AS nivel_aplicacao,
  CASE
    WHEN tipo_dado = 0 THEN 'string'
    WHEN tipo_dado = 1 THEN 'number'
    WHEN tipo_dado = 2 THEN 'date'
    ELSE 'unknown'
  END AS tipo_dado,
  created_at,
  updated_at
FROM staging.variaveis
WHERE deleted_at IS NULL