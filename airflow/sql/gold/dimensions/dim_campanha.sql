SELECT
  c.id_campanha,
  e.id_estudo AS fk_estudo,
  c.nome_campanha,
  c.data_inicio,
  c.data_atualizacao,
  c.descricao,
  COALESCE(vars.variaveis_customizadas, '{}'::jsonb) AS variaveis_customizadas
FROM public.silver_campanhas c
JOIN public.dim_estudo e ON e.id_estudo = c.fk_estudo_src
LEFT JOIN public.silver_variaveis_agregadas vars ON vars.id_nivel_aplicacao = c.id_campanha AND vars.nivel_aplicacao = 'campanha'