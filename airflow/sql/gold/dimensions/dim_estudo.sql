SELECT
  id_estudo,
  nome_estudo,
  '{}'::jsonb AS variaveis_customizadas  
FROM public.silver_estudos
