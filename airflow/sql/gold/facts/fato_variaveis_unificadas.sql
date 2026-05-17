-- Resolve a hierarquia polimórfica (id_nivel_aplicacao → id_registro) em 4 níveis (registro, evento, unidade, campanha)

WITH all_variables AS (
  -- 1. Nível Registro (Direto)
  SELECT
    r.id_registro,
    sv.variavel_id AS id_variavel,
    v.nivel_aplicacao AS nivel_hierarquico,
    v.nome AS nome_variavel,
    sv.valor_numerico,
    sv.valor_texto,
    sv.valor_data,
    sv.updated_at
  FROM public.silver_valores_variaveis sv
  JOIN public.dim_variavel v ON v.id = sv.variavel_id
  JOIN public.dim_registro_ocorrencia r ON r.id_registro = sv.id_nivel_aplicacao
  WHERE v.nivel_aplicacao = 'registro'

  UNION ALL

  -- 2. Nível Evento
  SELECT
    r.id_registro,
    sv.variavel_id AS id_variavel,
    v.nivel_aplicacao AS nivel_hierarquico,
    v.nome AS nome_variavel,
    sv.valor_numerico,
    sv.valor_texto,
    sv.valor_data,
    sv.updated_at
  FROM public.silver_valores_variaveis sv
  JOIN public.dim_variavel v ON v.id = sv.variavel_id
  JOIN public.dim_registro_ocorrencia r ON r.evento_amostragem_id = sv.id_nivel_aplicacao
  WHERE v.nivel_aplicacao = 'evento'

  UNION ALL

  -- 3. Nível Unidade
  SELECT
    r.id_registro,
    sv.variavel_id AS id_variavel,
    v.nivel_aplicacao AS nivel_hierarquico,
    v.nome AS nome_variavel,
    sv.valor_numerico,
    sv.valor_texto,
    sv.valor_data,
    sv.updated_at
  FROM public.silver_valores_variaveis sv
  JOIN public.dim_variavel v ON v.id = sv.variavel_id
  JOIN public.dim_evento_amostragem ev ON ev.fk_unidade_amostral = sv.id_nivel_aplicacao
  JOIN public.dim_registro_ocorrencia r ON r.evento_amostragem_id = ev.id_evento
  WHERE v.nivel_aplicacao = 'unidade'

  UNION ALL

  -- 4. Nível Campanha
  SELECT
    r.id_registro,
    sv.variavel_id AS id_variavel,
    v.nivel_aplicacao AS nivel_hierarquico,
    v.nome AS nome_variavel,
    sv.valor_numerico,
    sv.valor_texto,
    sv.valor_data,
    sv.updated_at
  FROM public.silver_valores_variaveis sv
  JOIN public.dim_variavel v ON v.id = sv.variavel_id
  JOIN public.dim_unidade_amostral u ON u.fk_campanha = sv.id_nivel_aplicacao
  JOIN public.dim_evento_amostragem ev ON ev.fk_unidade_amostral = u.id_unidade
  JOIN public.dim_registro_ocorrencia r ON r.evento_amostragem_id = ev.id_evento
  WHERE v.nivel_aplicacao = 'campanha'
)
-- DISTINCT ON: garante unicidade por (id_registro, id_variavel) com o valor mais recente
SELECT DISTINCT ON (id_registro, id_variavel)
  id_registro,
  id_variavel,
  nivel_hierarquico,
  nome_variavel,
  valor_numerico,
  valor_texto,
  valor_data,
  updated_at
FROM all_variables
ORDER BY id_registro, id_variavel, updated_at DESC
