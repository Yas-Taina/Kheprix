-- Gold: Dimensão Espécie
SELECT
  id_especie,
  nome_cientifico,
  nome_popular,
  classe,
  ordem,
  familia,
  genero,
  endemismo,
  status_conservacao
FROM public.silver_especies
