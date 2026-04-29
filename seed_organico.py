#!/usr/bin/env python3
"""
Popula a API Kheprix de forma organica via HTTP, exercitando todos os niveis
de estudo e cobrindo todos os status possiveis (convites, perfis, soft delete,
analises etc.).

Uso:
    python seed_organico.py [--base-url http://localhost:3000]

Pre-requisitos:
    - backend Rails rodando em http://localhost:3000 (docker compose up)
    - banco preferencialmente limpo (rails db:reset) para evitar emails
      duplicados; se ja existirem, o script faz fallback para login.

Sem dependencias externas (apenas stdlib).
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime, timedelta, timezone
from typing import Any
from urllib import error as urlerror
from urllib import request as urlreq


BASE_URL = "http://localhost:3000"

DONO_PRINCIPAL = {"nome": "Joao", "email": "chrisnotads2020@gmail.com", "senha": "senha123"}

OUTROS_USUARIOS = [
    {"nome": "Maria Colaboradora", "email": "maria.colab@example.com", "senha": "senha123"},
    {"nome": "Pedro Recusa",       "email": "pedro.recusa@example.com", "senha": "senha123"},
    {"nome": "Ana Cancelada",      "email": "ana.cancel@example.com",   "senha": "senha123"},
    {"nome": "Lucas Pendente",     "email": "lucas.pend@example.com",   "senha": "senha123"},
    {"nome": "Beatriz Codigo",     "email": "beatriz.cod@example.com",  "senha": "senha123"},
    {"nome": "Carlos Outro",       "email": "carlos.outro@example.com", "senha": "senha123"},
]


def http(method: str, path: str, token: str | None = None, body: dict | None = None) -> tuple[int, Any]:
    url = BASE_URL + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urlreq.Request(url, data=data, method=method, headers=headers)
    try:
        with urlreq.urlopen(req, timeout=60) as resp:
            raw = resp.read()
            ct = resp.headers.get("Content-Type", "")
            if "json" in ct and raw:
                return resp.status, json.loads(raw.decode("utf-8"))
            return resp.status, raw.decode("utf-8", errors="replace") if raw else ""
    except urlerror.HTTPError as e:
        raw = e.read()
        ct = e.headers.get("Content-Type", "") if e.headers else ""
        try:
            payload = json.loads(raw.decode("utf-8")) if "json" in ct and raw else raw.decode("utf-8", errors="replace")
        except Exception:
            payload = raw
        return e.code, payload


def step(label: str, status: int, payload: Any) -> None:
    flag = "OK " if 200 <= status < 300 else "ERR"
    summary = ""
    if isinstance(payload, dict):
        if "id" in payload:
            summary = f"id={payload['id']}"
        elif "token" in payload:
            summary = "token=***"
        elif "mensagem" in payload:
            summary = str(payload["mensagem"])[:60]
        elif "erro" in payload:
            summary = "erro=" + str(payload["erro"])[:80]
    elif isinstance(payload, list):
        summary = f"len={len(payload)}"
    print(f"  [{flag} {status}] {label}  {summary}")


def autocadastro(usuario: dict) -> tuple[int, Any]:
    return http("POST", "/usuarios/autocadastro", body=usuario)


def login(email: str, senha: str) -> str:
    code, payload = http("POST", "/autenticacao/login", body={"email": email, "senha": senha})
    if code != 200 or not isinstance(payload, dict) or "token" not in payload:
        raise RuntimeError(f"login falhou: {code} {payload}")
    return payload["token"]


def criar_ou_logar(usuario: dict) -> str:
    code, payload = autocadastro(usuario)
    step(f"autocadastro {usuario['email']}", code, payload)
    if code in (200, 201):
        return login(usuario["email"], usuario["senha"])
    return login(usuario["email"], usuario["senha"])


def main(base_url: str) -> int:
    global BASE_URL
    BASE_URL = base_url.rstrip("/")

    print(f"== Kheprix seed organico em {BASE_URL} ==\n")

    print("[1] Criando usuarios e fazendo login")
    tokens: dict[str, str] = {}
    tokens[DONO_PRINCIPAL["email"]] = criar_ou_logar(DONO_PRINCIPAL)
    for u in OUTROS_USUARIOS:
        tokens[u["email"]] = criar_ou_logar(u)
    joao = tokens[DONO_PRINCIPAL["email"]]
    maria = tokens["maria.colab@example.com"]
    pedro = tokens["pedro.recusa@example.com"]
    lucas = tokens["lucas.pend@example.com"]
    beatriz = tokens["beatriz.cod@example.com"]
    carlos = tokens["carlos.outro@example.com"]

    # ------------------------------------------------------------------ ESTUDO 1
    print("\n[2] Estudo 1 (dono: Joao) com variaveis em todos os niveis")
    code, est1 = http("POST", "/estudos", joao, {
        "nome": "Mata Atlantica - Nucleo Picinguaba",
        "observacoes": "Estudo de longo prazo sobre fauna terrestre",
        "variaveis": [
            {"nome": "Responsavel",   "nivel_aplicacao": "campanha", "tipo_dado": "string"},
            {"nome": "Tipo de solo",  "nivel_aplicacao": "unidade",  "tipo_dado": "string"},
            {"nome": "Temperatura",   "nivel_aplicacao": "evento",   "tipo_dado": "number", "metrica": "graus C"},
            {"nome": "Precipitacao",  "nivel_aplicacao": "evento",   "tipo_dado": "number", "metrica": "mm"},
            {"nome": "Comportamento", "nivel_aplicacao": "registro", "tipo_dado": "boolean"},
        ],
    })
    step("POST /estudos (Estudo 1)", code, est1)
    estudo_id = est1["id"]

    code, variaveis = http("GET", f"/estudos/{estudo_id}/variaveis", joao)
    step(f"GET /estudos/{estudo_id}/variaveis", code, variaveis)
    var_by_name = {v["nome"]: v for v in variaveis}
    var_responsavel  = var_by_name["Responsavel"]["id"]
    var_solo         = var_by_name["Tipo de solo"]["id"]
    var_temperatura  = var_by_name["Temperatura"]["id"]
    var_precipitacao = var_by_name["Precipitacao"]["id"]
    var_comportamento = var_by_name["Comportamento"]["id"]

    # ------------------------------------------------------------------ ESPECIES
    print("\n[3] Cadastrando especies")
    especies_payload = [
        {"classe": "Mammalia", "ordem": "Carnivora",   "familia": "Felidae",     "genero": "Panthera",  "especie": "onca",            "nome_popular": "Onca-pintada",        "status_conservacao": "Vulneravel",       "endemismo": False},
        {"classe": "Aves",     "ordem": "Passeriformes","familia": "Thraupidae",  "genero": "Tangara",   "especie": "seledon",         "nome_popular": "Saira-sete-cores",    "status_conservacao": "Pouco preocupante","endemismo": True},
        {"classe": "Mammalia", "ordem": "Pilosa",      "familia": "Bradypodidae", "genero": "Bradypus",  "especie": "torquatus",       "nome_popular": "Preguica-de-coleira", "status_conservacao": "Vulneravel",       "endemismo": True},
        {"classe": "Reptilia", "ordem": "Squamata",    "familia": "Viperidae",    "genero": "Bothrops",  "especie": "jararaca",        "nome_popular": "Jararaca",            "status_conservacao": "Pouco preocupante","endemismo": False},
        {"classe": "Insecta",  "ordem": "Hymenoptera", "familia": "Formicidae",   "genero": "Atta",      "especie": "laevigata",       "nome_popular": "Sauva",               "status_conservacao": "Pouco preocupante","endemismo": False},
    ]
    especies_ids: list[int] = []
    for sp in especies_payload:
        code, resp = http("POST", f"/estudos/{estudo_id}/especies", joao, sp)
        step(f"POST especie {sp['nome_popular']}", code, resp)
        especies_ids.append(resp["id"])

    code, _ = http("GET", f"/estudos/{estudo_id}/especies", joao)
    step("GET /especies", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/especies/{especies_ids[0]}", joao)
    step("GET /especies/:id", code, _)
    code, _ = http("PATCH", f"/estudos/{estudo_id}/especies/{especies_ids[1]}", joao,
                   {"nome_popular": "Saira-sete-cores (Mata Atlantica)"})
    step("PATCH especie", code, _)

    # ------------------------------------------------------------------ CAMPANHAS
    print("\n[4] Campanhas, unidades, eventos e registros")
    campanhas: list[dict] = []
    for nome_campanha, dt_ini, dt_fim, responsavel in [
        ("Campanha Verao 2026",  "2026-01-15", "2026-03-15", "Joao"),
        ("Campanha Outono 2026", "2026-04-01", None,         "Maria"),
    ]:
        body = {
            "nome": nome_campanha,
            "data_inicio": dt_ini,
            "descricao": f"Campanha {nome_campanha} no nucleo Picinguaba",
            "valores_variaveis": [{"variavel_id": var_responsavel, "valor": responsavel}],
        }
        if dt_fim:
            body["data_fim"] = dt_fim
        code, c = http("POST", f"/estudos/{estudo_id}/campanhas", joao, body)
        step(f"POST campanha {nome_campanha}", code, c)
        campanhas.append(c)

    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas", joao)
    step("GET campanhas", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", joao)
    step("GET campanha :id", code, _)
    # PATCH campanha - exercita updated_at e altera valores_variaveis
    code, _ = http("PATCH", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", joao, {
        "nome": campanhas[0]["nome"] + " (revisada)",
        "data_inicio": campanhas[0]["data_inicio"],
        "data_fim": campanhas[0].get("data_fim"),
        "descricao": "Descricao atualizada apos revisao",
        "valores_variaveis": [{"variavel_id": var_responsavel, "valor": "Joao (revisado)"}],
    })
    step("PATCH campanha[0]", code, _)

    # ------------------------------------------------------------------ UNIDADES
    unidades: list[dict] = []
    locais = [
        ("UA-01-Nascente", -23.36100, -44.83000, 50.0, "Armadilha fotografica", "30 dias de exposicao", "Argilo-arenoso"),
        ("UA-02-Trilha",   -23.36500, -44.82500, 75.0, "Busca ativa",            "8h/dia por 5 dias",   "Latossolo"),
        ("UA-03-Mangue",   -23.37000, -44.82000, 40.0, "Pitfall",                "10 armadilhas",       "Hidromorfico"),
    ]
    for c_idx, c in enumerate(campanhas):
        for nome, lat, lon, raio, metodo, esforco, solo in locais[: 2 + c_idx]:
            code, ua = http("POST", f"/estudos/{estudo_id}/campanhas/{c['id']}/unidades_amostrais", joao, {
                "nome": nome,
                "latitude":  str(lat),
                "longitude": str(lon),
                "raio": raio,
                "metodo_coleta": metodo,
                "esforco_amostral": esforco,
            })
            step(f"POST unidade {nome} (camp {c['id']})", code, ua)
            ua["_campanha_id"] = c["id"]
            ua["_solo"] = solo
            unidades.append(ua)

    # PATCH em uma unidade pra exercitar update
    code, _ = http("PATCH",
                   f"/estudos/{estudo_id}/campanhas/{unidades[0]['_campanha_id']}/unidades_amostrais/{unidades[0]['id']}",
                   joao, {
                       "nome": unidades[0]["nome"] + " (revisada)",
                       "latitude":  unidades[0]["latitude"],
                       "longitude": unidades[0]["longitude"],
                       "raio": 60.0,
                   })
    step("PATCH unidade[0]", code, _)

    # ------------------------------------------------------------------ EVENTOS
    eventos: list[dict] = []
    base_dt = datetime(2026, 2, 1, 8, 0, 0, tzinfo=timezone.utc)
    for u_idx, u in enumerate(unidades):
        for e_idx in range(2):
            inicio = base_dt + timedelta(days=u_idx * 5 + e_idx * 2)
            fim = inicio + timedelta(hours=4)
            code, ev = http("POST",
                            f"/estudos/{estudo_id}/campanhas/{u['_campanha_id']}/unidades_amostrais/{u['id']}/eventos_amostragem",
                            joao, {
                                "horario_inicio": inicio.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                                "horario_fim":    fim.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                                "esforco_real": f"{4 + e_idx}h de observacao direta",
                            })
            step(f"POST evento u={u['id']} #{e_idx+1}", code, ev)
            ev["_campanha_id"] = u["_campanha_id"]
            ev["_unidade_id"] = u["id"]
            eventos.append(ev)

    # PATCH evento (atualiza esforco) - DTO exige horario_inicio
    code, _ = http("PATCH",
                   f"/estudos/{estudo_id}/campanhas/{eventos[0]['_campanha_id']}/unidades_amostrais/{eventos[0]['_unidade_id']}/eventos_amostragem/{eventos[0]['id']}",
                   joao, {
                       "horario_inicio": eventos[0]["horario_inicio"],
                       "horario_fim":    eventos[0].get("horario_fim"),
                       "esforco_real": "5h (re-medido apos calibracao)",
                   })
    step("PATCH evento[0]", code, _)

    # ------------------------------------------------------------------ REGISTROS
    registros: list[dict] = []
    for ev_idx, ev in enumerate(eventos):
        cenarios = [
            {"especie_id": especies_ids[ev_idx % len(especies_ids)], "qtde_individuos": 3, "ausencia_especie": False},
            {"especie_id": especies_ids[(ev_idx + 1) % len(especies_ids)], "qtde_individuos": 1, "ausencia_especie": False},
            {"especie_id": especies_ids[(ev_idx + 2) % len(especies_ids)], "qtde_individuos": 0, "ausencia_especie": True},
        ]
        for r_idx, cen in enumerate(cenarios):
            code, reg = http("POST",
                             f"/estudos/{estudo_id}/campanhas/{ev['_campanha_id']}/unidades_amostrais/{ev['_unidade_id']}/eventos_amostragem/{ev['id']}/registro_ocorrencias",
                             joao, {
                                 "especie_id": cen["especie_id"],
                                 "data": "2026-02-15",
                                 "hora": f"{8 + r_idx:02d}:30:00",
                                 "latitude":  -23.36 + r_idx * 0.001,
                                 "longitude": -44.83 + r_idx * 0.001,
                                 "qtde_individuos": cen["qtde_individuos"],
                                 "ausencia_especie": cen["ausencia_especie"],
                             })
            step(f"POST registro ev={ev['id']} #{r_idx+1} ausencia={cen['ausencia_especie']}", code, reg)
            reg["_path"] = (ev["_campanha_id"], ev["_unidade_id"], ev["id"])
            registros.append(reg)

    # PATCH e DELETE em registros (status "editado" e "excluido")
    reg0 = registros[0]
    code, _ = http("PATCH",
                   f"/estudos/{estudo_id}/campanhas/{reg0['_path'][0]}/unidades_amostrais/{reg0['_path'][1]}/eventos_amostragem/{reg0['_path'][2]}/registro_ocorrencias/{reg0['id']}",
                   joao, {
                       "especie_id": reg0["especie_id"],
                       "data": reg0["data"],
                       "hora": reg0["hora"],
                       "latitude":  reg0["latitude"],
                       "longitude": reg0["longitude"],
                       "qtde_individuos": 5,
                       "ausencia_especie": False,
                   })
    step("PATCH registro[0]", code, _)
    code, _ = http("DELETE",
                   f"/estudos/{estudo_id}/campanhas/{registros[-1]['_path'][0]}/unidades_amostrais/{registros[-1]['_path'][1]}/eventos_amostragem/{registros[-1]['_path'][2]}/registro_ocorrencias/{registros[-1]['id']}",
                   joao)
    step("DELETE registro[-1] (soft delete)", code, _)

    # GET listas/detalhes
    ev0 = eventos[0]
    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas/{ev0['_campanha_id']}/unidades_amostrais/{ev0['_unidade_id']}/eventos_amostragem/{ev0['id']}/registro_ocorrencias", joao)
    step("GET registros (lista)", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas/{ev0['_campanha_id']}/unidades_amostrais/{ev0['_unidade_id']}/eventos_amostragem/{ev0['id']}/registro_ocorrencias/{registros[0]['id']}", joao)
    step("GET registro :id", code, _)

    # ------------------------------------------------------------------ CONVITES
    print("\n[5] Convites com todos os status")
    # 5.1 Aceito
    code, conv_aceito = http("POST", f"/estudos/{estudo_id}/convites", joao,
                             {"email_convidado": "maria.colab@example.com"})
    step("POST convite Maria (sera aceito)", code, conv_aceito)
    code, _ = http("POST", f"/convites/{conv_aceito['token']}/aceitar", maria)
    step("POST aceitar convite Maria", code, _)

    # 5.2 Recusado
    code, conv_recusado = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "pedro.recusa@example.com"})
    step("POST convite Pedro (sera recusado)", code, conv_recusado)
    code, _ = http("POST", f"/convites/{conv_recusado['token']}/recusar", pedro)
    step("POST recusar convite Pedro", code, _)

    # 5.3 Pendente (apenas cria)
    code, conv_pendente = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "lucas.pend@example.com"})
    step("POST convite Lucas (fica pendente)", code, conv_pendente)

    # 5.4 Cancelado pelo dono
    code, conv_cancelar = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "ana.cancel@example.com"})
    step("POST convite Ana (sera cancelado)", code, conv_cancelar)
    code, _ = http("DELETE", f"/estudos/{estudo_id}/convites/{conv_cancelar['id']}", joao)
    step("DELETE convite Ana (cancelar)", code, _)

    # GETs de convites
    code, _ = http("GET", f"/estudos/{estudo_id}/convites", joao)
    step("GET convites do estudo", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/convites?status=pendente", joao)
    step("GET convites status=pendente", code, _)
    code, _ = http("GET", "/convites", lucas)
    step("GET /convites recebidos (Lucas)", code, _)
    code, _ = http("GET", f"/convites/{conv_pendente['token']}")
    step("GET /convites/:token (publico)", code, _)

    # ------------------------------------------------------------------ COLABORADORES
    print("\n[6] Colaboradores")
    code, colabs = http("GET", f"/estudos/{estudo_id}/colaboradores", joao)
    step("GET colaboradores", code, colabs)
    maria_user_id = next((c["id_usuario"] for c in colabs if c["email"] == "maria.colab@example.com"), None)
    if maria_user_id:
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{maria_user_id}", joao,
                       {"perfil": "proprietario"})
        step("PATCH Maria -> proprietario", code, _)
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{maria_user_id}", joao,
                       {"perfil": "colaborador"})
        step("PATCH Maria -> colaborador (volta)", code, _)

    # ------------------------------------------------------------------ CODIGO ACESSO
    print("\n[7] Codigo de acesso e ingresso via codigo")
    code, codigo = http("GET", f"/estudos/{estudo_id}/codigo_acesso", joao)
    step("GET codigo acesso", code, codigo)
    nova_senha = "AcessoSeed2026"
    code, _ = http("PATCH", f"/estudos/{estudo_id}/codigo_acesso", joao,
                   {"senha_autocadastro": nova_senha})
    step("PATCH senha_autocadastro", code, _)
    code, _ = http("POST", "/estudos/ingressar", beatriz,
                   {"codigo": codigo["codigo"], "senha_autocadastro": nova_senha})
    step("POST /estudos/ingressar (Beatriz)", code, _)

    # ------------------------------------------------------------------ ESTUDO 2 (Carlos)
    print("\n[8] Estudo 2 (dono: Carlos) com Joao convidado e aceitando")
    code, est2 = http("POST", "/estudos", carlos, {
        "nome": "Cerrado - Chapada dos Veadeiros",
        "observacoes": "Estudo paralelo para testar visao de colaborador",
        "variaveis": [
            {"nome": "Pluviosidade", "nivel_aplicacao": "campanha", "tipo_dado": "number", "metrica": "mm"},
            {"nome": "Habitat",      "nivel_aplicacao": "unidade",  "tipo_dado": "string"},
        ],
    })
    step("POST /estudos (Estudo 2 - Carlos)", code, est2)
    estudo2_id = est2["id"]
    code, conv_joao = http("POST", f"/estudos/{estudo2_id}/convites", carlos,
                           {"email_convidado": DONO_PRINCIPAL["email"]})
    step("POST convite Joao -> Estudo 2", code, conv_joao)
    code, _ = http("POST", f"/convites/{conv_joao['token']}/aceitar", joao)
    step("POST aceitar convite Joao", code, _)

    # ------------------------------------------------------------------ ESTUDOS - GETs/filtros
    print("\n[9] Listagens e filtros de estudos")
    code, _ = http("GET", "/estudos", joao)
    step("GET /estudos (Joao)", code, _)
    code, _ = http("GET", "/estudos?nome=Mata", joao)
    step("GET /estudos?nome=Mata", code, _)
    hoje = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    code, _ = http("GET", f"/estudos?criado_a_partir_de=2026-01-01&criado_ate={hoje}", joao)
    step("GET /estudos com filtros de data", code, _)

    # ------------------------------------------------------------------ DASHBOARD
    print("\n[10] Dashboard")
    code, _ = http("GET", "/dashboard", joao)
    step("GET /dashboard (Joao)", code, _)
    code, _ = http("GET", "/dashboard", maria)
    step("GET /dashboard (Maria - colaboradora)", code, _)

    # ------------------------------------------------------------------ ANALISES
    print("\n[11] Analises (varios tipos cobrindo categorias do catalogo)")
    print("     NOTA: as analises consultam o Data Warehouse, que e populado")
    print("     pelo DAG do Airflow. Sem o DAG executado, e esperado 422 com")
    print("     'dados insuficientes'. As chamadas sao feitas para exercitar")
    print("     o endpoint (rota, autorizacao, validacao de chave).")
    analises_simples = ["shannon", "simpson", "margalef", "pielou", "berger_parker",
                        "brillouin", "macintosh", "hurlbert", "mcnaughton",
                        "chao1", "jackknife1", "jackknife2", "bootstrap", "ace",
                        "lognormal", "logserie", "geometrica", "vara_quebrada", "rarefacao"]
    for chave in analises_simples:
        code, _ = http("POST", f"/estudos/{estudo_id}/analises/executar", joao, {"chave": chave})
        step(f"analise {chave}", code, _)

    analises_amostra = ["chao2", "ice", "jaccard", "bray_curtis", "morisita", "sorensen", "nmds", "pca"]
    for chave in analises_amostra:
        code, _ = http("POST", f"/estudos/{estudo_id}/analises/executar", joao, {"chave": chave})
        step(f"analise {chave}", code, _)

    # Vetor unico (Shapiro)
    code, _ = http("POST", f"/estudos/{estudo_id}/analises/executar", joao,
                   {"chave": "shapiro", "variavel_id": var_temperatura})
    step("analise shapiro", code, _)

    # Dois vetores (Pearson, Spearman, Kendall, regressao_linear, GLMs)
    for chave in ["pearson", "spearman", "kendall", "regressao_linear",
                  "modelo_gaussiano", "modelo_gamma", "modelo_poisson", "modelo_binomial_negativa"]:
        code, _ = http("POST", f"/estudos/{estudo_id}/analises/executar", joao,
                       {"chave": chave, "variavel_x_id": var_temperatura, "variavel_y_id": var_precipitacao})
        step(f"analise {chave}", code, _)

    # Multiplos grupos (ANOVA / Kruskal) - agrupar por campanha
    for chave in ["anova", "kruskal"]:
        code, _ = http("POST", f"/estudos/{estudo_id}/analises/executar", joao,
                       {"chave": chave, "variavel_id": var_temperatura, "agrupar_por": "campanha"})
        step(f"analise {chave} (por campanha)", code, _)

    # ------------------------------------------------------------------ EXPORTACAO
    print("\n[12] Exportacao em todos os agrupamentos")
    for agrup in ["registro_ocorrencia", "evento_amostragem", "unidade_amostral", "campanha", "especie"]:
        code, _ = http("GET", f"/estudos/{estudo_id}/exportar_dados?agrupamento={agrup}", joao)
        step(f"exportar_dados agrupamento={agrup}", code, _)

    # ------------------------------------------------------------------ DELETES (soft)
    print("\n[13] Soft deletes para deixar entidades no estado 'excluido'")
    # Deletar uma especie (a ultima)
    code, _ = http("DELETE", f"/estudos/{estudo_id}/especies/{especies_ids[-1]}", joao)
    step("DELETE especie[-1]", code, _)
    # Deletar evento ANTES da unidade (senao a unidade some e o evento perde rota)
    ult_evento = eventos[-1]
    code, _ = http("DELETE",
                   f"/estudos/{estudo_id}/campanhas/{ult_evento['_campanha_id']}/unidades_amostrais/{ult_evento['_unidade_id']}/eventos_amostragem/{ult_evento['id']}",
                   joao)
    step("DELETE evento[-1]", code, _)
    # Deletar uma unidade (a ultima da ultima campanha) - usa uma unidade que ainda nao perdeu eventos
    ult_unidade = unidades[-1]
    code, _ = http("DELETE",
                   f"/estudos/{estudo_id}/campanhas/{ult_unidade['_campanha_id']}/unidades_amostrais/{ult_unidade['id']}",
                   joao)
    step("DELETE unidade[-1]", code, _)

    # ------------------------------------------------------------------ DASHBOARD final
    print("\n[14] Resumo")
    code, _ = http("GET", "/dashboard", joao)
    step("GET /dashboard final", code, _)
    code, lista_final = http("GET", "/estudos", joao)
    step("GET /estudos final", code, lista_final)

    print("\n== Concluido ==")
    print(f"Login: email={DONO_PRINCIPAL['email']}  senha={DONO_PRINCIPAL['senha']}")
    print(f"Estudo principal id={estudo_id}  '{est1['nome']}'")
    print(f"Estudo secundario id={estudo2_id} '{est2['nome']}'")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=BASE_URL)
    args = parser.parse_args()
    try:
        sys.exit(main(args.base_url))
    except Exception as e:
        print(f"\nFALHA: {e}")
        sys.exit(1)
