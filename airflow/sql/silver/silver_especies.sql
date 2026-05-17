SELECT
  id AS id_especie,
  -- TRIM evita espaços espúrios quando genero ou especie é NULL ('Apis ' ou ' mellifera')
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
