-- Gold: Dimensão Estudo
SELECT
  id_estudo,
  nome_estudo,
  '{}'::jsonb AS variaveis_customizadas  -- EAV não tem nível 'estudo'; campo existe por consistência com outras dims
FROM public.silver_estudos
