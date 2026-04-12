SELECT
  id AS id_registro,
  especie_id,
  evento_amostragem_id,
  data,
  latitude,
  longitude,
  ausencia_especie,
  qtde_individuos,
  updated_at AS data_atualizacao
FROM staging.registro_ocorrencias
WHERE deleted_at IS NULL
