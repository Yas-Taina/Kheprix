SELECT
  id AS id_evento,
  unidade_amostral_id AS fk_unidade_src,
  horario_inicio,
  updated_at AS data_atualizacao,
  esforco_real
FROM staging.eventos_amostragem
WHERE deleted_at IS NULL
