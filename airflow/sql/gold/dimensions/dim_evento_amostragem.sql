-- Gold: Dimensão Evento de Amostragem
SELECT
  ev.id_evento,
  u.id_unidade AS fk_unidade_amostral,
  ev.horario_inicio,
  ev.data_atualizacao,
  ev.esforco_real,
  COALESCE(vars.variaveis_customizadas, '{}'::jsonb) AS variaveis_customizadas
FROM public.silver_eventos_amostragem ev
JOIN public.dim_unidade_amostral u ON u.id_unidade = ev.fk_unidade_src
LEFT JOIN public.silver_variaveis_agregadas vars ON vars.id_nivel_aplicacao = ev.id_evento AND vars.nivel_aplicacao = 'evento'