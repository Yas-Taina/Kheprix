SELECT
  sv.id_nivel_aplicacao,
  v.nivel_aplicacao,
  jsonb_object_agg(v.nome, COALESCE(sv.valor_texto, sv.valor_numerico::text)) AS variaveis_customizadas
FROM (
  SELECT DISTINCT ON (id_nivel_aplicacao, variavel_id)
    id_nivel_aplicacao,
    variavel_id,
    valor_texto,
    valor_numerico
  FROM public.silver_valores_variaveis
  ORDER BY id_nivel_aplicacao, variavel_id, updated_at DESC
) sv
JOIN public.silver_variaveis v ON v.id = sv.variavel_id
GROUP BY sv.id_nivel_aplicacao, v.nivel_aplicacao
