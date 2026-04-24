# Endpoints da API

Todas as rotas estão na raiz (sem prefixo de namespace).

## Autenticação

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| POST /autenticacao/login | email(string), senha(string) | token(string) |
| POST /autenticacao/solicitar_redefinicao | email(string) | mensagem(string) |
| POST /autenticacao/validar_token_redefinicao | token(string) | valido(boolean) |
| POST /autenticacao/redefinir_senha | token(string), nova_senha(string) | mensagem(string) |

## Usuários

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| POST /usuarios/autocadastro | nome(string), email(string), senha(string) | id(int), nome(string), email(string), created_at(datetime) |

## Dashboard (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /dashboard | — | Com estudo disponível (200): id(int), nome(string), updated_at(datetime), data_inicio(date), total_registros(int), total_especies(int), especies_ameacadas(int), especies_nativas(int), especies_invasoras(int), total_individuos(int). O estudo é escolhido por `ultimo_estudo_acessado_id` do usuário; fallback para o estudo vinculado com `created_at` mais recente. Sem nenhum estudo vinculado (404): erro(string). |

## Dashboard do Estudo (autenticado, colaborador)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:id/dashboard | — | estudo: { id(int), nome(string), updated_at(datetime) }, resumo: { total_registros(int), total_especies(int), especies_ameacadas(int), especies_nativas(int), especies_invasoras(int), total_individuos(int), data_inicio(date) }, registros_por_data: array[ { data(date), total(int) } ], ocorrencias_por_especie: array[ { nome_cientifico(string), nome_popular(string), total(int) } ], pontos_mapa: array[ { latitude(decimal), longitude(decimal), nome_cientifico(string), quantidade(int) } ], registros_por_especie_tempo: array[ { ano(int), mes(int), nome_cientifico(string), is_endemica(boolean), total(int) } ], especies_distintas_por_mes: array[ { ano(int), mes(int), total(int) } ] |

## Estudos (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos | nome(string, opcional), criado_a_partir_de(date, opcional), criado_ate(date, opcional), atualizado_a_partir_de(date, opcional), atualizado_ate(date, opcional) | array[ id(int), nome(string), observacoes(string), perfil(string), created_at(datetime), updated_at(datetime) ] |
| POST /estudos | nome(string), observacoes(string, opcional), variaveis(array[ { nome(string), nivel_aplicacao(string), tipo_dado(string), metrica(string, opcional) } ]) | id(int), nome(string), observacoes(string), created_at(datetime), updated_at(datetime) |
| DELETE /estudos/:id | — | Proprietário único: 204 No Content (soft delete do estudo). Co-proprietário ou colaborador: 200 com mensagem(string) (descadastro do usuário). |

## Variáveis (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/variaveis | nivel_aplicacao(string, opcional: campanha\|unidade\|evento\|registro) | array[ id(int), nome(string), metrica(string), nivel_aplicacao(string), tipo_dado(string), created_at(datetime), updated_at(datetime) ] |

## Espécies (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/especies | nome_popular(string, opcional) | array[ id(int), estudo_id(int), foto(string), classe(string), ordem(string), familia(string), genero(string), especie(string), nome_popular(string), status_conservacao(string), endemismo(boolean), created_at(datetime) ] |
| GET /estudos/:estudo_id/especies/:id | — | id(int), estudo_id(int), foto(string), classe(string), ordem(string), familia(string), genero(string), especie(string), nome_popular(string), status_conservacao(string), endemismo(boolean), created_at(datetime) |
| POST /estudos/:estudo_id/especies | classe(string), ordem(string), familia(string), genero(string), especie(string), endemismo(boolean), foto(string, opcional), nome_popular(string, opcional), status_conservacao(string, opcional) | id(int), estudo_id(int), foto(string), classe(string), ordem(string), familia(string), genero(string), especie(string), nome_popular(string), status_conservacao(string), endemismo(boolean), created_at(datetime) |
| PATCH /estudos/:estudo_id/especies/:id | foto(string, opcional), classe(string, opcional), ordem(string, opcional), familia(string, opcional), genero(string, opcional), especie(string, opcional), nome_popular(string, opcional), status_conservacao(string, opcional), endemismo(boolean, opcional) | id(int), estudo_id(int), foto(string), classe(string), ordem(string), familia(string), genero(string), especie(string), nome_popular(string), status_conservacao(string), endemismo(boolean), created_at(datetime) |
| DELETE /estudos/:estudo_id/especies/:id | — | 204 No Content |

## Colaboradores (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/colaboradores | — | array[ id_usuario(int), nome(string), email(string), perfil(string) ] |
| PATCH /estudos/:estudo_id/colaboradores/:id | perfil(string) | id_usuario(int), nome(string), email(string), perfil(string) |
| DELETE /estudos/:estudo_id/colaboradores/:id | — | 204 No Content |

## Convites (autenticado, proprietário)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| POST /estudos/:estudo_id/convites | email_convidado(string) | id(int), estudo_id(int), email_convidado(string), token(string), status(string), data_expiracao(datetime), created_at(datetime) |
| GET /estudos/:estudo_id/convites | status(string, opcional) | array[ id(int), email_convidado(string), status(string), data_expiracao(datetime), created_at(datetime) ] |
| DELETE /estudos/:estudo_id/convites/:id | — | 204 No Content |

## Convites Recebidos (autenticado, exceto show que é público)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /convites | — | array[ id(int), estudo_id(int), nome_estudo(string), nome_remetente(string), status(string), data_expiracao(datetime), created_at(datetime) ] |
| GET /convites/:token | — | id(int), estudo_id(int), nome_estudo(string), email_convidado(string), status(string), data_expiracao(datetime) |
| POST /convites/:token/aceitar | — | mensagem(string) |
| POST /convites/:token/recusar | — | mensagem(string) |

## Código de Acesso (autenticado, proprietário)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/codigo_acesso | — | codigo(string), senha_autocadastro(string) |
| PATCH /estudos/:estudo_id/codigo_acesso | senha_autocadastro(string) | codigo(string), senha_autocadastro(string) |

## Autocadastro em Estudo (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| POST /estudos/ingressar | codigo(string), senha_autocadastro(string) | estudo_id(int), nome_estudo(string), perfil(string) |

## Campanhas (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas | — | array[ id(int), nome(string), data_inicio(date), descricao(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) ] |
| GET /estudos/:estudo_id/campanhas/:id | — | id(int), nome(string), data_inicio(date), descricao(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| POST /estudos/:estudo_id/campanhas | nome(string), data_inicio(date), descricao(string, opcional), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) — id NÃO permitido | id(int), nome(string), data_inicio(date), descricao(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| PATCH /estudos/:estudo_id/campanhas/:id | nome(string), data_inicio(date), descricao(string), valores_variaveis(array[ { id(int, obrigatório), valor(string) } ], opcional) — itens omitidos viram soft-delete | id(int), nome(string), data_inicio(date), descricao(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| DELETE /estudos/:estudo_id/campanhas/:id | — | 204 No Content |

## Unidades Amostrais (autenticado, proprietário para exclusão)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais | — | array[ id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | — | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais | nome(string), latitude(decimal), longitude(decimal), raio(decimal, opcional), metodo_coleta(string, opcional), esforco_amostral(string, opcional), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) — id NÃO permitido | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | nome(string), latitude(decimal), longitude(decimal), raio(decimal, opcional), metodo_coleta(string, opcional), esforco_amostral(string, opcional), valores_variaveis(array[ { id(int, obrigatório), valor(string) } ], opcional) — itens omitidos viram soft-delete | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | — | 204 No Content |

## Eventos de Amostragem (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem | — | array[ id(int), unidade_amostral_id(int), horario_inicio(datetime), esforco_real(string), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | — | id(int), unidade_amostral_id(int), horario_inicio(datetime), esforco_real(string), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem | horario_inicio(datetime), esforco_real(string), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) — id NÃO permitido | id(int), unidade_amostral_id(int), horario_inicio(datetime), esforco_real(string), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | horario_inicio(datetime), esforco_real(string), valores_variaveis(array[ { id(int, obrigatório), valor(string) } ], opcional) — itens omitidos viram soft-delete | id(int), unidade_amostral_id(int), horario_inicio(datetime), esforco_real(string), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | — | 204 No Content |

## Registros de Ocorrência (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias | — | array[ id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | — | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias | especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int, opcional), foto(string, opcional), ausencia_especie(boolean, opcional), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) — id NÃO permitido | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | especie_id(int, opcional), data(date, opcional), hora(time, opcional), latitude(decimal, opcional), longitude(decimal, opcional), qtde_individuos(int, opcional), foto(string, opcional), ausencia_especie(boolean, opcional), valores_variaveis(array[ { id(int, obrigatório), valor(string) } ], opcional) — itens omitidos viram soft-delete | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime), valores_variaveis(array[ { id(int), variavel_id(int), valor(string) } ]) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | — | 204 No Content |

## Fotos (público)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /fotos/estudos/:estudo_id/:tipo/:arquivo | — | Arquivo de imagem (inline). 404 se não encontrado. |

> **Nota sobre o campo `foto`:** Nos endpoints de Espécies e Registros de Ocorrência, o campo `foto` aceita uma string base64 da imagem no envio. Na resposta, retorna o URL path do arquivo salvo (ex.: `"/fotos/estudos/1/especies/uuid.png"`), que pode ser acessado via o endpoint acima.
>
> Formatos aceitos: JPEG, PNG, WebP. O formato é detectado automaticamente pelo header `data:image/<tipo>;base64,...`.

### Exemplos de base64 por formato

**JPEG** (foto de animal, 80x80px):
```
data:image/jpeg;base64,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
```

**WebP** (foto de paisagem, 80x80px):
```
data:image/webp;base64,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
```

**PNG** (aceita também o formato padrão sem header):
```
iVBORw0KGgoAAAANSUhEUg...
```
Neste caso o sistema assume extensão `.png` por padrão.
## Exportação de Dados (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:id/exportar_dados | formato(string, obrigatório: csv\|xml), agrupamento(string, obrigatório quando formato=csv: registro_ocorrencia\|evento_amostragem\|unidade_amostral\|campanha\|especie) | 200 arquivo CSV (Content-Type: text/csv) ou XML (Content-Type: application/xml). Para formato XML, o parâmetro agrupamento é ignorado — a hierarquia completa aninhada é sempre exportada. 422 parâmetros inválidos. 403 não é colaborador do estudo. |

## Análises (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| POST /estudos/:estudo_id/analises/executar | chave(string, obrigatório), variavel_ids(array int, opcional), variavel_x_id(int, opcional), variavel_y_id(int, opcional), variavel_id(int, opcional), agrupar_por(string, opcional: campanha \| unidade_amostral \| evento_amostragem \| mes \| ano \| estacao), grupo1_ids(array int, opcional), grupo2_ids(array int, opcional), nome_grupo1(string, opcional), nome_grupo2(string, opcional), campanha_ids(array int, opcional), unidade_ids(array int, opcional), evento_ids(array int, opcional), data_inicio(string ISO8601, opcional), data_fim(string ISO8601, opcional), latitude_min(decimal, opcional), latitude_max(decimal, opcional), longitude_min(decimal, opcional), longitude_max(decimal, opcional), fonte(string, opcional: variavel \| abundancia \| riqueza), fonte_x(string, opcional: idem fonte), fonte_y(string, opcional: idem fonte), nivel_agregacao(string, opcional: campanha \| unidade_amostral \| evento) | 200: { analise(string), nome(string), valor(object\|null), grafico(string html\|null) }. 422: chave inválida, dados insuficientes ou parâmetro inválido (fonte/agrupar_por/bounding box). 403: não é colaborador do estudo. 404: estudo não encontrado. |
| GET /analises/estudos/:estudo_id/:chave/:arquivo | — | 200 arquivo ZIP (Content-Type: application/zip) contendo `resultado.json` (sempre), `resultado.xml` (sempre) e `resultado.html` (somente quando a análise retorna gráfico). 401 sem token. 403 não é colaborador do estudo. 404 arquivo ou estudo inexistente. |

### Chaves de análise disponíveis

Lista espelhada de `app/models/catalogo_analise.rb`. Ao adicionar uma análise nova lá, atualize também esta seção.

```ts
type ChaveAnalise =
  | "lognormal" | "logserie" | "geometrica" | "vara_quebrada"
  | "rarefacao"
  | "jackknife1" | "jackknife2" | "chao1" | "chao2" | "bootstrap" | "ace" | "ice"
  | "shannon" | "simpson" | "margalef" | "pielou" | "berger_parker"
  | "brillouin" | "macintosh" | "hurlbert" | "mcnaughton"
  | "teste_t" | "ks" | "shapiro" | "anova" | "kruskal"
  | "pearson" | "spearman" | "kendall"
  | "regressao_linear"
  | "jaccard" | "bray_curtis" | "morisita" | "sorensen"
  | "rda" | "cca" | "nmds" | "pca"
  | "modelo_gaussiano" | "modelo_gamma" | "modelo_poisson" | "modelo_binomial_negativa"
  | "michaelis_menten";

type CategoriaAnalise =
  | "modelo_distribuicao" | "rarefacao" | "estimador_riqueza" | "indice_diversidade"
  | "teste_hipotese" | "correlacao" | "regressao" | "similaridade"
  | "multivariada" | "glm" | "acumulacao";

type TipoDadoAnalise =
  | "abundancias" | "abundancias_por_amostra" | "abundancias_com_variaveis"
  | "matriz_acumulacao" | "dois_vetores" | "vetor_unico"
  | "dois_grupos" | "multiplos_grupos";

interface Analise {
  chave: ChaveAnalise;
  nome: string;
  categoria: CategoriaAnalise;
  tipo_dado: TipoDadoAnalise;
  tem_valor: boolean;   // a análise retorna o campo `valor` preenchido
  tem_grafico: boolean; // a análise retorna o campo `grafico` preenchido
}

const CATALOGO_ANALISES: readonly Analise[] = [
  { chave: "lognormal",                nome: "Log-Normal",              categoria: "modelo_distribuicao", tipo_dado: "abundancias",              tem_valor: false, tem_grafico: true  },
  { chave: "logserie",                 nome: "Log-Série",               categoria: "modelo_distribuicao", tipo_dado: "abundancias",              tem_valor: false, tem_grafico: true  },
  { chave: "geometrica",               nome: "Geométrica",              categoria: "modelo_distribuicao", tipo_dado: "abundancias",              tem_valor: false, tem_grafico: true  },
  { chave: "vara_quebrada",            nome: "Vara Quebrada",           categoria: "modelo_distribuicao", tipo_dado: "abundancias",              tem_valor: false, tem_grafico: true  },
  { chave: "rarefacao",                nome: "Rarefação",               categoria: "rarefacao",           tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "jackknife1",               nome: "Jackknife 1ª Ordem",      categoria: "estimador_riqueza",   tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "jackknife2",               nome: "Jackknife 2ª Ordem",      categoria: "estimador_riqueza",   tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "chao1",                    nome: "Chao1",                   categoria: "estimador_riqueza",   tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "chao2",                    nome: "Chao2",                   categoria: "estimador_riqueza",   tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: false },
  { chave: "bootstrap",                nome: "Bootstrap",               categoria: "estimador_riqueza",   tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "ace",                      nome: "ACE",                     categoria: "estimador_riqueza",   tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "ice",                      nome: "ICE",                     categoria: "estimador_riqueza",   tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: false },
  { chave: "shannon",                  nome: "Shannon-Wiener",          categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "simpson",                  nome: "Simpson",                 categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "margalef",                 nome: "Margalef",                categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "pielou",                   nome: "Pielou",                  categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "berger_parker",            nome: "Berger-Parker",           categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "brillouin",                nome: "Brillouin",               categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "macintosh",                nome: "McIntosh",                categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "hurlbert",                 nome: "Hurlbert",                categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "mcnaughton",               nome: "McNaughton",              categoria: "indice_diversidade",  tipo_dado: "abundancias",              tem_valor: true,  tem_grafico: false },
  { chave: "teste_t",                  nome: "Teste T",                 categoria: "teste_hipotese",      tipo_dado: "dois_grupos",              tem_valor: true,  tem_grafico: false },
  { chave: "ks",                       nome: "Kolmogorov-Smirnov",      categoria: "teste_hipotese",      tipo_dado: "dois_grupos",              tem_valor: true,  tem_grafico: false },
  { chave: "shapiro",                  nome: "Shapiro-Wilk",            categoria: "teste_hipotese",      tipo_dado: "vetor_unico",              tem_valor: true,  tem_grafico: false },
  { chave: "anova",                    nome: "ANOVA",                   categoria: "teste_hipotese",      tipo_dado: "multiplos_grupos",         tem_valor: true,  tem_grafico: false },
  { chave: "kruskal",                  nome: "Kruskal-Wallis",          categoria: "teste_hipotese",      tipo_dado: "multiplos_grupos",         tem_valor: true,  tem_grafico: false },
  { chave: "pearson",                  nome: "Correlação de Pearson",   categoria: "correlacao",          tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "spearman",                 nome: "Correlação de Spearman",  categoria: "correlacao",          tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "kendall",                  nome: "Correlação de Kendall",   categoria: "correlacao",          tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "regressao_linear",         nome: "Regressão Linear",        categoria: "regressao",           tipo_dado: "dois_vetores",             tem_valor: false, tem_grafico: true  },
  { chave: "jaccard",                  nome: "Índice de Jaccard",       categoria: "similaridade",        tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: true  },
  { chave: "bray_curtis",              nome: "Bray-Curtis",             categoria: "similaridade",        tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: true  },
  { chave: "morisita",                 nome: "Morisita",                categoria: "similaridade",        tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: true  },
  { chave: "sorensen",                 nome: "Sørensen",                categoria: "similaridade",        tipo_dado: "abundancias_por_amostra",  tem_valor: true,  tem_grafico: true  },
  { chave: "rda",                      nome: "RDA",                     categoria: "multivariada",        tipo_dado: "abundancias_com_variaveis", tem_valor: false, tem_grafico: true  },
  { chave: "cca",                      nome: "CCA",                     categoria: "multivariada",        tipo_dado: "abundancias_com_variaveis", tem_valor: false, tem_grafico: true  },
  { chave: "nmds",                     nome: "nMDS",                    categoria: "multivariada",        tipo_dado: "abundancias_por_amostra",  tem_valor: false, tem_grafico: true  },
  { chave: "pca",                      nome: "PCA",                     categoria: "multivariada",        tipo_dado: "abundancias_por_amostra",  tem_valor: false, tem_grafico: true  },
  { chave: "modelo_gaussiano",         nome: "GLM Gaussiano",           categoria: "glm",                 tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "modelo_gamma",             nome: "GLM Gamma",               categoria: "glm",                 tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "modelo_poisson",           nome: "GLM Poisson",             categoria: "glm",                 tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "modelo_binomial_negativa", nome: "GLM Binomial Negativa",   categoria: "glm",                 tipo_dado: "dois_vetores",             tem_valor: true,  tem_grafico: true  },
  { chave: "michaelis_menten",         nome: "Michaelis-Menten",        categoria: "acumulacao",          tipo_dado: "matriz_acumulacao",        tem_valor: false, tem_grafico: true  },
];
```

### Filtros globais

Todos os filtros abaixo são opcionais e aditivos (AND). Aplicam-se a qualquer `tipo_dado`, restringindo o universo de registros que entra na análise.

| Filtro | Tipo | Descrição |
|--------|------|-----------|
| `campanha_ids` | array int | Restringe a essas campanhas. |
| `unidade_ids` | array int | Restringe a essas unidades amostrais. |
| `evento_ids` | array int | Restringe a esses eventos de amostragem. |
| `data_inicio`, `data_fim` | ISO8601 | Janela de datas do registro (`data_registro`). |
| `latitude_min`, `latitude_max`, `longitude_min`, `longitude_max` | decimal | Bounding box geográfico aplicado sobre `dim_unidade_amostral`. Cada coordenada é opcional (podem ser usadas isoladamente). |

### Fontes derivadas

Análises numéricas (`vetor_unico`, `dois_vetores`, `dois_grupos`, `multiplos_grupos`) aceitam, além de variáveis customizadas, fontes derivadas dos registros:

- `abundancia` → `SUM(abundancia)` agrupada por `nivel_agregacao`.
- `riqueza` → `COUNT(DISTINCT especie)` agrupada por `nivel_agregacao`, excluindo ausências (`abundancia <= 0`).
- `variavel` (default) → valor da variável customizada (`variavel_id`, `variavel_x_id`, `variavel_y_id`).

`nivel_agregacao` aceita `campanha`, `unidade_amostral` (default) e `evento`. Em `multiplos_grupos` com fonte derivada, cada evento vira uma observação e o grupo vem do `agrupar_por`.

### Parâmetros por tipo_dado

| tipo_dado | Parâmetros obrigatórios (fonte=variavel) | Parâmetros obrigatórios (fonte derivada) |
|-----------|------------------------------------------|------------------------------------------|
| abundancias | — | (n/a, sempre usa registros) |
| abundancias_por_amostra | — | (n/a) |
| matriz_acumulacao | — | (n/a) |
| abundancias_com_variaveis | variavel_ids | (n/a) |
| dois_vetores | variavel_x_id, variavel_y_id | — (fonte_x/fonte_y) |
| vetor_unico | variavel_id | — (fonte) |
| dois_grupos | variavel_id, grupo1_ids, grupo2_ids (nome_grupo1, nome_grupo2 opcionais) | grupo1_ids, grupo2_ids (fonte) |
| multiplos_grupos | variavel_id, agrupar_por | agrupar_por (fonte) |

Valores aceitos em `agrupar_por`:

- **Hierárquicos**: `campanha`, `unidade_amostral`, `evento_amostragem`
- **Temporais**: `mes` (`YYYY-MM`), `ano` (`YYYY`), `estacao` (Verão/Outono/Inverno/Primavera do hemisfério sul; dezembro cai no verão do ano seguinte)
