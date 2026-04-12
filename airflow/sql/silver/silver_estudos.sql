SELECT
  id AS id_estudo,
  nome AS nome_estudo,
  updated_at
FROM staging.estudos
WHERE deleted_at IS NULL