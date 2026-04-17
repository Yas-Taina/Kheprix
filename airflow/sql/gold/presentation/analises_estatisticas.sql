-- id_variavel = 0 para registros sem variável associada (LEFT JOIN null row)
SELECT
  f.id_registro,
  COALESCE(var.id_variavel, 0) AS id_variavel,
  est.id_estudo,
  est.nome_estudo,
  f.fk_campanha,
  cam.nome_campanha,
  f.fk_unidade_amostral,
  u.nome_unidade_amostral,
  f.fk_evento,
  dt.data_completa AS data_registro,
  dt.ano,
  dt.mes,
  esp.nome_cientifico AS especie,
  COALESCE(esp.ordem, 'NA') AS ordem,
  COALESCE(esp.familia, 'NA') AS familia,
  f.quantidade AS abundancia,
  COALESCE(var.nivel_hierarquico, 'NA') AS nivel_variavel,
  COALESCE(var.nome_variavel, 'NA') AS nome_variavel,
  var.valor_numerico,
  var.valor_texto,
  var.valor_data
FROM public.fato_medicao_entomologica f
JOIN public.dim_especie esp ON esp.id_especie = f.fk_especie
JOIN public.dim_estudo est ON est.id_estudo = f.fk_estudo
JOIN public.dim_campanha cam ON cam.id_campanha = f.fk_campanha
JOIN public.dim_unidade_amostral u ON u.id_unidade  = f.fk_unidade_amostral
JOIN public.dim_tempo dt ON dt.id_data    = f.fk_data
LEFT JOIN public.fato_variaveis_unificadas var ON var.id_registro = f.id_registro
