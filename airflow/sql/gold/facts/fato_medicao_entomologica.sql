-- Fato: Medição Entomológica
SELECT
  r.id_registro,
  dt.id_data AS fk_data,
  esp.id_especie AS fk_especie,
  c.fk_estudo AS fk_estudo,
  c.id_campanha AS fk_campanha,
  u.id_unidade AS fk_unidade_amostral,
  ev.id_evento AS fk_evento,
  r.latitude,
  r.longitude,
  r.quantidade_apurada AS quantidade
FROM public.dim_registro_ocorrencia r
JOIN public.dim_tempo dt ON dt.data_completa = r.data
JOIN public.dim_especie esp ON esp.id_especie = r.especie_id
JOIN public.dim_evento_amostragem ev ON r.evento_amostragem_id = ev.id_evento
JOIN public.dim_unidade_amostral u ON ev.fk_unidade_amostral = u.id_unidade
JOIN public.dim_campanha c ON u.fk_campanha = c.id_campanha
