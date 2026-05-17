SELECT
  u.id_unidade,
  c.id_campanha AS fk_campanha,
  u.nome_unidade_amostral,
  u.latitude,
  u.longitude,
  u.raio,
  u.metodo_coleta,
  u.esforco_amostral,
  COALESCE(vars.variaveis_customizadas, '{}'::jsonb) AS variaveis_customizadas
FROM public.silver_unidades_amostrais u
JOIN public.dim_campanha c ON c.id_campanha = u.fk_campanha_src
LEFT JOIN public.silver_variaveis_agregadas vars ON vars.id_nivel_aplicacao = u.id_unidade AND vars.nivel_aplicacao = 'unidade'
