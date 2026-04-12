SELECT
  id AS id_campanha,
  estudo_id AS fk_estudo_src,
  nome AS nome_campanha,
  data_inicio,
  updated_at AS data_atualizacao,
  descricao
FROM staging.campanhas
WHERE deleted_at IS NULL
