SELECT
  id AS id_unidade,
  campanha_id AS fk_campanha_src,
  latitude,
  longitude,
  raio,
  metodo_coleta,
  esforco_amostral
FROM staging.unidades_amostrais
WHERE deleted_at IS NULL
