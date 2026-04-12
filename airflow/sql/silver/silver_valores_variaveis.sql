-- Silver: Valores de Variáveis
-- Converte o valor bruto (text) para o tipo correto com base em silver_variaveis.tipo_dado.
-- CAST protegido por regex: valor malformado retorna NULL em vez de derrubar a task.
SELECT
  vv.id,
  vv.variavel_id,
  vv.id_nivel_aplicacao,
  CASE
    WHEN v.tipo_dado = 'number' AND vv.valor ~ '^-?[0-9]*\.?[0-9]+$'
      THEN CAST(vv.valor AS DECIMAL(18,4))
    ELSE NULL
  END AS valor_numerico,
  CASE
    WHEN v.tipo_dado IN ('string', 'date') THEN vv.valor
    ELSE NULL
  END AS valor_texto,
  CASE
    WHEN v.tipo_dado = 'date' AND vv.valor ~ '^\d{4}-\d{2}-\d{2}$'
      THEN CAST(vv.valor AS DATE)
    ELSE NULL
  END AS valor_data,
  vv.updated_at
FROM staging.valores_variaveis vv
JOIN public.silver_variaveis v ON v.id = vv.variavel_id
WHERE vv.deleted_at IS NULL
