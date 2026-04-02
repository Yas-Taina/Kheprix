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
| GET /estudos/:estudo_id/campanhas | — | array[ id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) ] |
| GET /estudos/:estudo_id/campanhas/:id | — | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| POST /estudos/:estudo_id/campanhas | nome(string), data_inicio(date), data_fim(date, opcional), descricao(string, opcional), valores_variaveis(array[ { variavel_id(int), valor(string) } ], opcional) | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| PATCH /estudos/:estudo_id/campanhas/:id | nome(string), data_inicio(date), data_fim(date), descricao(string), valores_variaveis(array[ { variavel_id(int), valor(string) } ]) | id(int), nome(string), data_inicio(date), data_fim(date), descricao(string), created_at(datetime), updated_at(datetime) |
| DELETE /estudos/:estudo_id/campanhas/:id | — | 204 No Content |

## Unidades Amostrais (autenticado, proprietário para exclusão)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais | — | array[ id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | — | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais | nome(string), latitude(decimal), longitude(decimal), raio(decimal, opcional), metodo_coleta(string, opcional), esforco_amostral(string, opcional) | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | nome(string), latitude(decimal), longitude(decimal), raio(decimal, opcional), metodo_coleta(string, opcional), esforco_amostral(string, opcional) | id(int), campanha_id(int), nome(string), latitude(decimal), longitude(decimal), raio(decimal), metodo_coleta(string), esforco_amostral(string), created_at(datetime), updated_at(datetime) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:id | — | 204 No Content |

## Eventos de Amostragem (autenticado, proprietário para escrita)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem | — | array[ id(int), unidade_amostral_id(int), horario_inicio(datetime), horario_fim(datetime), esforco_real(string), created_at(datetime) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | — | id(int), unidade_amostral_id(int), horario_inicio(datetime), horario_fim(datetime), esforco_real(string), created_at(datetime) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem | horario_inicio(datetime), horario_fim(datetime, opcional), esforco_real(string, opcional) | id(int), unidade_amostral_id(int), horario_inicio(datetime), horario_fim(datetime), esforco_real(string), created_at(datetime) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | horario_inicio(datetime), horario_fim(datetime, opcional), esforco_real(string, opcional) | id(int), unidade_amostral_id(int), horario_inicio(datetime), horario_fim(datetime), esforco_real(string), created_at(datetime) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:id | — | 204 No Content |

## Registros de Ocorrência (autenticado)

| Rota | Dados enviados | Dados recebidos |
|------|----------------|-----------------|
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias | — | array[ id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime) ] |
| GET /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | — | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime) |
| POST /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias | especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int, opcional), foto(string, opcional), ausencia_especie(boolean, opcional) | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime) |
| PATCH /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | especie_id(int, opcional), data(date, opcional), hora(time, opcional), latitude(decimal, opcional), longitude(decimal, opcional), qtde_individuos(int, opcional), foto(string, opcional), ausencia_especie(boolean, opcional) | id(int), evento_amostragem_id(int), especie_id(int), data(date), hora(time), latitude(decimal), longitude(decimal), qtde_individuos(int), foto(string), ausencia_especie(boolean), created_at(datetime) |
| DELETE /estudos/:estudo_id/campanhas/:campanha_id/unidades_amostrais/:unidade_amostral_id/eventos_amostragem/:evento_amostragem_id/registro_ocorrencias/:id | — | 204 No Content |
