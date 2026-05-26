SELECT
  id AS id_especie,
  TRIM(CONCAT(COALESCE(genero, ''), ' ', COALESCE(especie, ''))) AS nome_cientifico,
  nome_popular,
  classe,
  ordem,
  familia,
  genero,
  endemismo,
  status_conservacao
FROM staging.especies
WHERE deleted_at IS NULL
