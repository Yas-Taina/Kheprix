SELECT
  f.id_registro,
  f.fk_estudo,
  est.nome_estudo,
  cam.nome_campanha,
  f.latitude,
  f.longitude,
  t.data_completa AS data_registro,
  t.ano,
  t.mes,
  t.estacao,
  esp.nome_cientifico,
  COALESCE(esp.nome_popular, 'NA') AS nome_popular,
  COALESCE(esp.ordem, 'NA') AS ordem,
  COALESCE(esp.familia, 'NA') AS familia,
  COALESCE(esp.genero, 'NA') AS genero,
  COALESCE(esp.status_conservacao, 'NA') AS status_conservacao,
  f.quantidade,
  COALESCE(esp.endemismo, false) AS is_endemica,
  CASE
    WHEN esp.status_conservacao IN ('CR', 'EN', 'VU') THEN TRUE
    ELSE FALSE
  END AS is_ameacada
FROM public.fato_medicao_entomologica f
JOIN public.dim_tempo t ON t.id_data = f.fk_data
JOIN public.dim_especie esp ON esp.id_especie = f.fk_especie
JOIN public.dim_estudo est ON est.id_estudo = f.fk_estudo
JOIN public.dim_campanha cam ON cam.id_campanha = f.fk_campanha
