# Endpoints da API

Todas as rotas são prefixadas com `/api/v1`.

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

## Estudos (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos | nome(string, opcional), criado_a_partir_de(date, opcional), criado_ate(date, opcional) | array[ id(int), nome(string), observacoes(string), perfil(string), created_at(datetime), updated_at(datetime) ] |
| POST /estudos | nome(string), observacoes(string, opcional), variaveis(array[ { nome(string), nivel_aplicacao(string), tipo_dado(string), metrica(string, opcional) } ]) | id(int), nome(string), observacoes(string), created_at(datetime) |
| DELETE /estudos/:id | — | 204 No Content |

## Espécies (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/especies | nome_popular(string, opcional), nome_cientifico(string, opcional) | array[ id(int), estudo_id(int), foto(string), classe(string), genero(string), nome_popular(string), nome_cientifico(string), status_conservacao(string), nativa_da_regiao(boolean), created_at(datetime) ] |
| GET /estudos/:estudo_id/especies/:id | — | id(int), estudo_id(int), foto(string), classe(string), genero(string), nome_popular(string), nome_cientifico(string), status_conservacao(string), nativa_da_regiao(boolean), created_at(datetime) |
| POST /estudos/:estudo_id/especies | nome_cientifico(string), nome_popular(string, opcional), foto(string, opcional), classe(string, opcional), genero(string, opcional), status_conservacao(string, opcional), nativa_da_regiao(boolean, opcional) | id(int), estudo_id(int), foto(string), classe(string), genero(string), nome_popular(string), nome_cientifico(string), status_conservacao(string), nativa_da_regiao(boolean), created_at(datetime) |
| PATCH /estudos/:estudo_id/especies/:id | nome_cientifico(string), nome_popular(string), foto(string), classe(string), genero(string), status_conservacao(string), nativa_da_regiao(boolean) | id(int), estudo_id(int), foto(string), classe(string), genero(string), nome_popular(string), nome_cientifico(string), status_conservacao(string), nativa_da_regiao(boolean), created_at(datetime) |
| DELETE /estudos/:estudo_id/especies/:id | — | 204 No Content |

## Campanhas (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas | — | array[ id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) ] |
| GET /estudos/:estudo_id/campanhas/:id | — | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| POST /estudos/:estudo_id/campanhas | nome(string), data_inicio(date), data_fim(date, opcional), descricao(string, opcional), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| PUT /estudos/:estudo_id/campanhas/:id | nome(string), data_inicio(date), data_fim(date), descricao(string), valores_variaveis(array[ { variavel_id(int), valor(string) } ]) | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| DELETE /estudos/:estudo_id/campanhas/:id | — | 204 No Content |
