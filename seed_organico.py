#!/usr/bin/env python3
"""
Popula a API Kheprix de forma organica via HTTP.

Dados dimensionados para cobrir todas as 38 analises disponiveis na API R:
  - 12 especies (incl. singletons e doubletons para Chao1/Chao2)
  - 9 campanhas: 3 anos (2024/2025/2026) x 3 estacoes (verao/outono/inverno)
  - 3 grupos sazonais para ANOVA; series temporais para analises interanuais
  - 8 unidades por campanha = 72 totais (matrizes, nMDS, RDA/CCA)
  - 3 eventos por unidade = 216 eventos (distribuidos em varios meses e anos)
  - ~1300 registros de ocorrencia cobrindo 8 localizacoes ao longo de 3 anos
  - Variaveis em todos os niveis: campanha(1), unidade(3), evento(3), registro(1)

Uso:
    python seed_organico.py [--base-url http://localhost:3000]

Pre-requisitos:
    - backend Rails rodando (docker compose up)
    - banco preferencialmente limpo (rails db:reset)

Sem dependencias externas (apenas stdlib).
"""

from __future__ import annotations

import argparse
import base64
import json
import struct
import sys
import zlib
from datetime import datetime, timezone
from typing import Any
from urllib import error as urlerror
from urllib import request as urlreq


BASE_URL = "http://localhost:3000"

DONO_PRINCIPAL = {"nome": "Joao", "email": "chrisnotads2020@gmail.com", "senha": "senha123"}

OUTROS_USUARIOS = [
    {"nome": "Maria Colaboradora", "email": "maria.colab@example.com",  "senha": "senha123"},
    {"nome": "Pedro Recusa",       "email": "pedro.recusa@example.com", "senha": "senha123"},
    {"nome": "Ana Cancelada",      "email": "ana.cancel@example.com",   "senha": "senha123"},
    {"nome": "Lucas Pendente",     "email": "lucas.pend@example.com",   "senha": "senha123"},
    {"nome": "Beatriz Codigo",     "email": "beatriz.cod@example.com",  "senha": "senha123"},
    {"nome": "Carlos Outro",       "email": "carlos.outro@example.com", "senha": "senha123"},
]


# ── Imagens ───────────────────────────────────────────────────────────────────

def _chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def png_b64(r: int, g: int, b: int) -> str:
    """PNG 1x1 colorido usado como fallback quando o download falha."""
    png = (
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
        + _chunk(b"IDAT", zlib.compress(b"\x00" + bytes([r, g, b])))
        + _chunk(b"IEND", b"")
    )
    return "data:image/png;base64," + base64.b64encode(png).decode()


_img_cache: dict[str, str] = {}


def imagem_b64(url: str, fallback: tuple[int, int, int] = (128, 128, 128)) -> str:
    """Baixa imagem de URL e retorna como data URI base64; cache em memoria."""
    if url in _img_cache:
        return _img_cache[url]
    try:
        req = urlreq.Request(url, headers={"User-Agent": "KheprixSeed/1.0"})
        with urlreq.urlopen(req, timeout=15) as resp:
            data = resp.read()
            ct = resp.headers.get("Content-Type", "image/jpeg").split(";")[0].strip()
            if not ct.startswith("image/"):
                ct = "image/jpeg"
            result = f"data:{ct};base64," + base64.b64encode(data).decode()
    except Exception as exc:
        print(f"    [WARN] download falhou ({url[:70]}): {exc}")
        result = png_b64(*fallback)
    _img_cache[url] = result
    return result


# (url, cor_fallback_rgb) — fotos reais das 12 especies via Wikipedia Commons
_ESPECIES_IMG: list[tuple[str, tuple[int, int, int]]] = [
    # Atta laevigata — Sauva-cabeca-de-vidro
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Atta_laevigata.jpg?width=400",            ( 80, 140,  60)),
    # Dichotomius geminatus — Besouro-rola-bosta
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Dichotomius_geminatus.jpg?width=400",     ( 60,  60, 140)),
    # Heliconius erato — Borboleta-erato
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Heliconius_erato_cyrbia_-_Chicaque.jpg?width=400", (220, 140, 40)),
    # Erythrodiplax fusca — Libelula-parda
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Erythrodiplax_fusca.jpg?width=400",       ( 40, 140, 200)),
    # Melipona scutellaris — Abelha-urucu
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Melipona_scutellaris.jpg?width=400",      (220, 200,  60)),
    # Nasutitermes corniger — Cupim-soldado
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Nasutitermes_corniger.jpg?width=400",     (180,  60,  60)),
    # Gryllus assimilis — Grilo-do-campo
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Gryllus_assimilis.jpg?width=400",         ( 60, 180, 180)),
    # Attacus atlas — Mariposa-atlas
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Attacus_atlas_qtl1.jpg?width=400",        (180,  60, 180)),
    # Aphis gossypii — Pulgao-verde
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Aphis_gossypii.jpg?width=400",            (100, 180, 100)),
    # Wyeomyia mitchellii — Mosquito-bromelicula (singleton A)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Wyeomyia_mitchellii.jpg?width=400",       (210, 210, 210)),
    # Frankliniella schultzei — Tripes-das-flores (singleton B)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Frankliniella_schultzei.jpg?width=400",   (255, 180, 200)),
    # Empoasca kraemeri — Cigarrinha-verde (Chao2 Q2)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Empoasca_kraemeri.jpg?width=400",         (140, 110,  70)),
]

# 20 fotos variadas de campo para registros de ocorrencia (picsum.photos — deterministico)
_N_FOTOS_CAMPO = 20
_URLS_CAMPO = [f"https://picsum.photos/seed/{300 + i}/480/360" for i in range(_N_FOTOS_CAMPO)]


# ── Definicoes de campanhas e locais ─────────────────────────────────────────
# Cada campanha: (nome, data_inicio, data_fim, responsavel,
#                 datas_eventos[(yr,mo,dy)x3],
#                 base_temp, base_precip, base_humid)
CAMPANHAS_DEF = [
    # ── 2024 ──────────────────────────────────────────────────────────────────
    ("Campanha Verao 2024",    "2024-01-10", "2024-03-31", "Joao",
     [(2024, 1, 10), (2024, 2,  8), (2024, 3,  4)], 29.0, 148.0, 80.0),
    ("Campanha Outono 2024",   "2024-04-02", "2024-06-28", "Maria",
     [(2024, 4,  3), (2024, 5,  8), (2024, 6,  6)], 23.5, 100.0, 70.0),
    ("Campanha Inverno 2024",  "2024-07-03", "2024-09-27", "Beatriz",
     [(2024, 7, 10), (2024, 8, 14), (2024, 9,  5)], 16.5,  58.0, 59.0),
    # ── 2025 ──────────────────────────────────────────────────────────────────
    ("Campanha Verao 2025",    "2025-01-12", "2025-03-31", "Joao",
     [(2025, 1, 12), (2025, 2,  9), (2025, 3,  6)], 29.5, 152.0, 81.0),
    ("Campanha Outono 2025",   "2025-04-04", "2025-06-30", "Maria",
     [(2025, 4,  6), (2025, 5, 12), (2025, 6, 10)], 23.8, 102.0, 70.5),
    ("Campanha Inverno 2025",  "2025-07-05", "2025-09-29", "Beatriz",
     [(2025, 7,  7), (2025, 8, 11), (2025, 9,  2)], 16.8,  60.0, 60.0),
    # ── 2026 ──────────────────────────────────────────────────────────────────
    ("Campanha Verao 2026",    "2026-01-15", "2026-03-31", "Joao",
     [(2026, 1, 15), (2026, 2, 10), (2026, 3,  5)], 30.0, 155.0, 82.0),
    ("Campanha Outono 2026",   "2026-04-01", "2026-06-30", "Maria",
     [(2026, 4,  5), (2026, 5, 10), (2026, 6,  8)], 24.0, 105.0, 71.0),
    ("Campanha Inverno 2026",  "2026-07-01", "2026-09-30", "Beatriz",
     [(2026, 7,  8), (2026, 8, 12), (2026, 9,  3)], 17.0,  62.0, 61.0),
]

# Cada local: (nome, lat, lon, raio, metodo, esforco, solo, altitude_m, cobertura_pct)
LOCAIS = [
    ("UA-01-Nascente",   -23.361, -44.830, 80.0, "Armadilha fotografica", "30 dias de exposicao",  "Argilo-arenoso", 120.0, 75.0),
    ("UA-02-Trilha",     -23.365, -44.825, 75.0, "Busca ativa",           "8h/dia por 5 dias",     "Latossolo",      200.0, 60.0),
    ("UA-03-Mangue",     -23.370, -44.820, 40.0, "Pitfall",               "10 armadilhas/30 dias", "Hidromorfico",    30.0, 90.0),
    ("UA-04-Clareira",   -23.355, -44.835, 60.0, "Malaise",               "15 dias continuo",      "Podzolico",      180.0, 45.0),
    ("UA-05-Ribeirinha", -23.358, -44.840, 50.0, "Winkler",               "5 amostras de solo",    "Aluvial",         60.0, 85.0),
    ("UA-06-Gruta",      -23.375, -44.815, 35.0, "Armadilha fotografica", "20 dias de exposicao",  "Calcario",       250.0, 30.0),
    ("UA-07-Cerrado",    -23.348, -44.845, 90.0, "Busca ativa",           "8h/dia por 7 dias",     "Latossolo",      310.0, 40.0),
    ("UA-08-Brejo",      -23.380, -44.810, 55.0, "Pitfall",               "15 armadilhas/30 dias", "Gleissolo",       20.0, 95.0),
]


# ── HTTP helper ───────────────────────────────────────────────────────────────

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


def login(email: str, senha: str) -> str:
    code, payload = http("POST", "/autenticacao/login", body={"email": email, "senha": senha})
    if code != 200 or not isinstance(payload, dict) or "token" not in payload:
        raise RuntimeError(f"login falhou: {code} {payload}")
    return payload["token"]


def criar_ou_logar(usuario: dict) -> str:
    code, payload = http("POST", "/usuarios/autocadastro", body=usuario)
    step(f"autocadastro {usuario['email']}", code, payload)
    return login(usuario["email"], usuario["senha"])


def main(base_url: str) -> int:
    global BASE_URL
    BASE_URL = base_url.rstrip("/")
    print(f"== Kheprix seed organico em {BASE_URL} ==\n")

    # ── [0] Download de imagens reais ────────────────────────────────────────
    print("[0] Baixando imagens reais ...")
    fotos_especies = [imagem_b64(url, fb) for url, fb in _ESPECIES_IMG]
    fotos_campo    = [imagem_b64(url) for url in _URLS_CAMPO]
    print(f"  {len(fotos_especies)} fotos de especies | {len(fotos_campo)} fotos de campo prontas\n")

    # ── [1] Usuarios ──────────────────────────────────────────────────────────
    print("[1] Criando usuarios e fazendo login")
    tokens: dict[str, str] = {}
    tokens[DONO_PRINCIPAL["email"]] = criar_ou_logar(DONO_PRINCIPAL)
    for u in OUTROS_USUARIOS:
        tokens[u["email"]] = criar_ou_logar(u)
    joao    = tokens[DONO_PRINCIPAL["email"]]
    maria   = tokens["maria.colab@example.com"]
    pedro   = tokens["pedro.recusa@example.com"]
    lucas   = tokens["lucas.pend@example.com"]
    beatriz = tokens["beatriz.cod@example.com"]
    carlos  = tokens["carlos.outro@example.com"]

    # ── [2] Estudo 1 com variaveis em todos os niveis ─────────────────────────
    print("\n[2] Estudo 1 com variaveis em todos os niveis")
    code, est1 = http("POST", "/estudos", joao, {
        "nome": "Entomofauna - Mata Atlantica Nucleo Picinguaba",
        "observacoes": "Estudo de longo prazo sobre insetos terrestres",
        "variaveis": [
            {"nome": "Responsavel",       "nivel_aplicacao": "campanha", "tipo_dado": "string"},
            {"nome": "Tipo de solo",      "nivel_aplicacao": "unidade",  "tipo_dado": "string"},
            {"nome": "Altitude",          "nivel_aplicacao": "unidade",  "tipo_dado": "number", "metrica": "metros"},
            {"nome": "Cobertura vegetal", "nivel_aplicacao": "unidade",  "tipo_dado": "number", "metrica": "%"},
            {"nome": "Temperatura",       "nivel_aplicacao": "evento",   "tipo_dado": "number", "metrica": "graus C"},
            {"nome": "Precipitacao",      "nivel_aplicacao": "evento",   "tipo_dado": "number", "metrica": "mm"},
            {"nome": "Umidade",           "nivel_aplicacao": "evento",   "tipo_dado": "number", "metrica": "%"},
            {"nome": "Comportamento",     "nivel_aplicacao": "registro", "tipo_dado": "boolean"},
        ],
    })
    step("POST /estudos (Estudo 1)", code, est1)
    estudo_id = est1["id"]

    code, variaveis = http("GET", f"/estudos/{estudo_id}/variaveis", joao)
    step("GET /variaveis", code, variaveis)
    vbn              = {v["nome"]: v["id"] for v in variaveis}
    var_responsavel  = vbn["Responsavel"]
    var_solo         = vbn["Tipo de solo"]
    var_altitude     = vbn["Altitude"]
    var_cobertura    = vbn["Cobertura vegetal"]
    var_temperatura  = vbn["Temperatura"]
    var_precipitacao = vbn["Precipitacao"]
    var_umidade      = vbn["Umidade"]
    var_comportamento = vbn["Comportamento"]

    # ── [3] Especies (12 com foto) ────────────────────────────────────────────
    print("\n[3] Cadastrando 12 especies com foto")
    especies_payload = [
        {"classe": "Insecta", "ordem": "Hymenoptera",  "familia": "Formicidae",   "genero": "Atta",          "especie": "laevigata",  "nome_popular": "Sauva-cabeca-de-vidro",  "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Coleoptera",   "familia": "Scarabaeidae", "genero": "Dichotomius",   "especie": "geminatus",  "nome_popular": "Besouro-rola-bosta",     "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Lepidoptera",  "familia": "Nymphalidae",  "genero": "Heliconius",    "especie": "erato",      "nome_popular": "Borboleta-erato",        "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Odonata",      "familia": "Libellulidae", "genero": "Erythrodiplax", "especie": "fusca",      "nome_popular": "Libelula-parda",         "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Hymenoptera",  "familia": "Apidae",       "genero": "Melipona",      "especie": "scutellaris","nome_popular": "Abelha-urucu",           "status_conservacao": "Vulneravel",          "endemismo": True},
        {"classe": "Insecta", "ordem": "Isoptera",     "familia": "Termitidae",   "genero": "Nasutitermes",  "especie": "corniger",   "nome_popular": "Cupim-soldado",          "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Orthoptera",   "familia": "Gryllidae",    "genero": "Gryllus",       "especie": "assimilis",  "nome_popular": "Grilo-do-campo",         "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Lepidoptera",  "familia": "Saturniidae",  "genero": "Attacus",       "especie": "atlas",      "nome_popular": "Mariposa-atlas",         "status_conservacao": "Vulneravel",          "endemismo": False},
        {"classe": "Insecta", "ordem": "Hemiptera",    "familia": "Aphididae",    "genero": "Aphis",         "especie": "gossypii",   "nome_popular": "Pulgao-verde",           "status_conservacao": "Pouco preocupante",   "endemismo": False},
        # Singletons Chao1 (total de 1 individuo em todo o estudo)
        {"classe": "Insecta", "ordem": "Diptera",      "familia": "Culicidae",    "genero": "Wyeomyia",      "especie": "mitchellii", "nome_popular": "Mosquito-bromelicula",   "status_conservacao": "Dados insuficientes", "endemismo": True},
        {"classe": "Insecta", "ordem": "Thysanoptera", "familia": "Thripidae",    "genero": "Frankliniella", "especie": "schultzei",  "nome_popular": "Tripes-das-flores",      "status_conservacao": "Pouco preocupante",   "endemismo": False},
        # Doubleton Chao2 (aparece em 2 unidades distintas, 1 reg em cada)
        {"classe": "Insecta", "ordem": "Hemiptera",    "familia": "Cicadellidae", "genero": "Empoasca",      "especie": "kraemeri",   "nome_popular": "Cigarrinha-verde",       "status_conservacao": "Pouco preocupante",   "endemismo": False},
    ]
    especies_ids: list[int] = []
    for sp_idx, sp in enumerate(especies_payload):
        code, resp = http("POST", f"/estudos/{estudo_id}/especies", joao,
                          {**sp, "foto": fotos_especies[sp_idx]})
        step(f"POST especie {sp['nome_popular']}", code, resp)
        especies_ids.append(resp["id"])

    code, _ = http("GET", f"/estudos/{estudo_id}/especies", joao)
    step("GET /especies", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/especies/{especies_ids[0]}", joao)
    step("GET /especies/:id", code, _)
    code, _ = http("PATCH", f"/estudos/{estudo_id}/especies/{especies_ids[1]}", joao,
                   {"nome_popular": "Besouro-rola-bosta (Mata Atlantica)"})
    step("PATCH especie[1]", code, _)

    # ── [4] Campanhas (3 sazonais) ────────────────────────────────────────────
    print("\n[4] Campanhas, unidades, eventos e registros")
    campanhas: list[dict] = []
    for camp_def in CAMPANHAS_DEF:
        camp_nome, dt_ini, dt_fim, resp_nome = camp_def[0], camp_def[1], camp_def[2], camp_def[3]
        code, camp = http("POST", f"/estudos/{estudo_id}/campanhas", joao, {
            "nome":        camp_nome,
            "data_inicio": dt_ini,
            "data_fim":    dt_fim,
            "descricao":   f"Campanha {camp_nome} - Nucleo Picinguaba",
            "valores_variaveis": [{"variavel_id": var_responsavel, "valor": resp_nome}],
        })
        step(f"POST campanha {camp_nome}", code, camp)
        campanhas.append(camp)

    # PATCH campanha[0]
    vv0_id = ((campanhas[0].get("valores_variaveis") or [{}])[0]).get("id")
    code, _ = http("PATCH", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", joao, {
        "nome":        campanhas[0]["nome"] + " (revisada)",
        "data_inicio": campanhas[0]["data_inicio"],
        "data_fim":    campanhas[0].get("data_fim"),
        "descricao":   "Descricao atualizada apos revisao",
        "valores_variaveis": [{"id": vv0_id, "valor": "Joao (revisado)"}] if vv0_id else [],
    })
    step("PATCH campanha[0]", code, _)

    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas", joao)
    step("GET campanhas", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", joao)
    step("GET campanha :id", code, _)

    # ── Unidades (5 por campanha = 15 total) ──────────────────────────────────
    # unidade_map[(c_idx, u_idx)] = unidade dict
    unidade_map: dict[tuple[int, int], dict] = {}
    for c_idx, campanha in enumerate(campanhas):
        for u_idx, local in enumerate(LOCAIS):
            nome, lat, lon, raio, metodo, esforco, solo, alt, cob = local
            code, ua = http("POST",
                f"/estudos/{estudo_id}/campanhas/{campanha['id']}/unidades_amostrais",
                joao, {
                    "nome":             nome,
                    "latitude":         str(lat),
                    "longitude":        str(lon),
                    "raio":             raio,
                    "metodo_coleta":    metodo,
                    "esforco_amostral": esforco,
                    "valores_variaveis": [
                        {"variavel_id": var_solo,      "valor": solo},
                        {"variavel_id": var_altitude,  "valor": str(alt)},
                        {"variavel_id": var_cobertura, "valor": str(cob)},
                    ],
                })
            step(f"POST unidade c{c_idx} {nome}", code, ua)
            ua["_campanha_id"] = campanha["id"]
            unidade_map[(c_idx, u_idx)] = ua

    # PATCH unidade[0][0]
    ua00 = unidade_map[(0, 0)]
    vvs_ua00 = {vv["variavel_id"]: vv["id"] for vv in (ua00.get("valores_variaveis") or [])}
    patch_ua_vv = []
    for var_id, new_val in [
        (var_solo,      "Argilo-arenoso (revisado)"),
        (var_altitude,  "125.0"),
        (var_cobertura, "77.0"),
    ]:
        if vvs_ua00.get(var_id):
            patch_ua_vv.append({"id": vvs_ua00[var_id], "valor": new_val})
    code, _ = http("PATCH",
        f"/estudos/{estudo_id}/campanhas/{ua00['_campanha_id']}/unidades_amostrais/{ua00['id']}",
        joao, {
            "nome":      ua00["nome"] + " (revisada)",
            "latitude":  ua00["latitude"],
            "longitude": ua00["longitude"],
            "raio":      85.0,
            "valores_variaveis": patch_ua_vv,
        })
    step("PATCH unidade[0][0]", code, _)

    # ── Eventos (3 por unidade = 45 total) ────────────────────────────────────
    # evento_map[(c_idx, u_idx, e_idx)] = evento dict
    evento_map: dict[tuple[int, int, int], dict] = {}
    for c_idx, (campanha, camp_def) in enumerate(zip(campanhas, CAMPANHAS_DEF)):
        datas_ev   = camp_def[4]
        base_temp  = camp_def[5]
        base_precip = camp_def[6]
        base_humid = camp_def[7]
        for u_idx in range(len(LOCAIS)):
            ua = unidade_map[(c_idx, u_idx)]
            for e_idx, (yr, mo, dy) in enumerate(datas_ev):
                temp   = round(base_temp   + u_idx * 0.4 - e_idx * 0.5, 1)
                precip = round(base_precip + u_idx * 3.0 - e_idx * 8.0, 1)
                humid  = round(base_humid  + u_idx * 0.8 - e_idx * 1.5, 1)
                inicio = datetime(yr, mo, dy,  8, 0, 0, tzinfo=timezone.utc)
                fim    = datetime(yr, mo, dy, 12, 0, 0, tzinfo=timezone.utc)
                code, ev = http("POST",
                    f"/estudos/{estudo_id}/campanhas/{campanha['id']}/unidades_amostrais/{ua['id']}/eventos_amostragem",
                    joao, {
                        "horario_inicio": inicio.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                        "horario_fim":    fim.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                        "esforco_real":   f"{4 + e_idx}h de observacao",
                        "valores_variaveis": [
                            {"variavel_id": var_temperatura,  "valor": str(temp)},
                            {"variavel_id": var_precipitacao, "valor": str(precip)},
                            {"variavel_id": var_umidade,      "valor": str(humid)},
                        ],
                    })
                step(f"POST evento c{c_idx}u{u_idx}e{e_idx}", code, ev)
                ev["_campanha_id"] = campanha["id"]
                ev["_unidade_id"]  = ua["id"]
                evento_map[(c_idx, u_idx, e_idx)] = ev

    # PATCH evento[0][0][0]
    ev000 = evento_map[(0, 0, 0)]
    vvs_ev000 = {vv["variavel_id"]: vv["id"] for vv in (ev000.get("valores_variaveis") or [])}
    patch_ev_vv = []
    for var_id, new_val in [
        (var_temperatura,  "31.2"),
        (var_precipitacao, "148.0"),
        (var_umidade,      "84.0"),
    ]:
        if vvs_ev000.get(var_id):
            patch_ev_vv.append({"id": vvs_ev000[var_id], "valor": new_val})
    code, _ = http("PATCH",
        f"/estudos/{estudo_id}/campanhas/{ev000['_campanha_id']}/unidades_amostrais/{ev000['_unidade_id']}/eventos_amostragem/{ev000['id']}",
        joao, {
            "horario_inicio": ev000["horario_inicio"],
            "horario_fim":    ev000.get("horario_fim"),
            "esforco_real":   "5h (re-medido apos calibracao)",
            "valores_variaveis": patch_ev_vv,
        })
    step("PATCH evento[0][0][0]", code, _)

    # ── Registros ─────────────────────────────────────────────────────────────
    # Matriz de abundancia deterministica:
    #   sp 0-8 (comuns): presentes se hash(c,u,e,sp) % 3 != 0  (~67%)
    #   sp 9  (singleton A): apenas c=0, u=0, e=0, qtde=1
    #   sp 10 (singleton B): apenas c=0, u=1, e=0, qtde=1
    #   sp 11 (Chao2 Q2):    c=0,u=0,e=2  E  c=0,u=2,e=0, qtde=1 cada

    registros: list[dict] = []

    for c_idx in range(len(campanhas)):
        camp_def = CAMPANHAS_DEF[c_idx]
        datas_ev = camp_def[4]
        for u_idx in range(len(LOCAIS)):
            _, lat, lon = LOCAIS[u_idx][0], LOCAIS[u_idx][1], LOCAIS[u_idx][2]
            for e_idx in range(3):
                ev       = evento_map[(c_idx, u_idx, e_idx)]
                camp_id  = ev["_campanha_id"]
                unit_id  = ev["_unidade_id"]
                ev_id    = ev["id"]
                yr, mo, dy = datas_ev[e_idx]
                data_str = f"{yr}-{mo:02d}-{dy:02d}"

                def post_reg(sp_id: int, qtde: int, comp: str, ausencia: bool = False, foto_idx: int = 0) -> dict:
                    body = {
                        "especie_id":       sp_id,
                        "data":             data_str,
                        "hora":             "09:00:00",
                        "latitude":         lat + 0.0001,
                        "longitude":        lon + 0.0001,
                        "qtde_individuos":  qtde,
                        "ausencia_especie": ausencia,
                        "foto":             fotos_campo[foto_idx % _N_FOTOS_CAMPO],
                        "valores_variaveis": [
                            {"variavel_id": var_comportamento, "valor": comp},
                        ],
                    }
                    code2, r = http("POST",
                        f"/estudos/{estudo_id}/campanhas/{camp_id}/unidades_amostrais/{unit_id}/eventos_amostragem/{ev_id}/registro_ocorrencias",
                        joao, body)
                    step(f"POST reg c{c_idx}u{u_idx}e{e_idx} sp_id={sp_id}", code2, r)
                    if isinstance(r, dict):
                        r["_path"] = (camp_id, unit_id, ev_id)
                        registros.append(r)
                        return r
                    return {}

                # Especies comuns (0-8)
                for sp_idx in range(9):
                    if (u_idx * 7 + e_idx * 3 + c_idx * 11 + sp_idx * 13) % 3 != 0:
                        qtde     = ((c_idx + u_idx * 2 + e_idx + sp_idx * 3) % 5) + 2
                        comp     = "true" if (sp_idx + e_idx) % 2 == 0 else "false"
                        foto_idx = c_idx * 100 + u_idx * 10 + e_idx * 3 + sp_idx
                        post_reg(especies_ids[sp_idx], qtde, comp, foto_idx=foto_idx)

                # Especies raras
                if c_idx == 0 and u_idx == 0 and e_idx == 0:
                    post_reg(especies_ids[9],  1, "true",  foto_idx=0)   # singleton A — Chao1 f1
                if c_idx == 0 and u_idx == 1 and e_idx == 0:
                    post_reg(especies_ids[10], 1, "false", foto_idx=1)   # singleton B — Chao1 f1
                if (c_idx == 0 and u_idx == 0 and e_idx == 2) or \
                   (c_idx == 0 and u_idx == 2 and e_idx == 0):
                    post_reg(especies_ids[11], 1, "true",  foto_idx=2)   # Chao2 Q2 (2 unidades)

    # PATCH registro[0]
    if registros:
        reg0 = registros[0]
        vv_id0 = ((reg0.get("valores_variaveis") or [{}])[0]).get("id")
        code, _ = http("PATCH",
            f"/estudos/{estudo_id}/campanhas/{reg0['_path'][0]}/unidades_amostrais/{reg0['_path'][1]}/eventos_amostragem/{reg0['_path'][2]}/registro_ocorrencias/{reg0['id']}",
            joao, {
                "especie_id":      reg0["especie_id"],
                "data":            reg0["data"],
                "hora":            reg0["hora"],
                "latitude":        reg0["latitude"],
                "longitude":       reg0["longitude"],
                "qtde_individuos": 8,
                "ausencia_especie": False,
                "valores_variaveis": [{"id": vv_id0, "valor": "false"}] if vv_id0 else [],
            })
        step("PATCH registro[0]", code, _)

    if len(registros) >= 2:
        reg_last = registros[-1]
        code, _ = http("DELETE",
            f"/estudos/{estudo_id}/campanhas/{reg_last['_path'][0]}/unidades_amostrais/{reg_last['_path'][1]}/eventos_amostragem/{reg_last['_path'][2]}/registro_ocorrencias/{reg_last['id']}",
            joao)
        step("DELETE registro[-1] (soft delete)", code, _)

    ev_s = evento_map[(0, 0, 0)]
    code, _ = http("GET",
        f"/estudos/{estudo_id}/campanhas/{ev_s['_campanha_id']}/unidades_amostrais/{ev_s['_unidade_id']}/eventos_amostragem/{ev_s['id']}/registro_ocorrencias",
        joao)
    step("GET registros (lista)", code, _)

    # ── [5] Convites ──────────────────────────────────────────────────────────
    print("\n[5] Convites com todos os status")
    code, conv_aceito = http("POST", f"/estudos/{estudo_id}/convites", joao,
                             {"email_convidado": "maria.colab@example.com"})
    step("POST convite Maria (sera aceito)", code, conv_aceito)
    code, _ = http("POST", f"/convites/{conv_aceito['token']}/aceitar", maria)
    step("POST aceitar convite Maria", code, _)

    code, conv_recusado = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "pedro.recusa@example.com"})
    step("POST convite Pedro (sera recusado)", code, conv_recusado)
    code, _ = http("POST", f"/convites/{conv_recusado['token']}/recusar", pedro)
    step("POST recusar convite Pedro", code, _)

    code, conv_pendente = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "lucas.pend@example.com"})
    step("POST convite Lucas (fica pendente)", code, conv_pendente)

    code, conv_cancelar = http("POST", f"/estudos/{estudo_id}/convites", joao,
                               {"email_convidado": "ana.cancel@example.com"})
    step("POST convite Ana (sera cancelado)", code, conv_cancelar)
    code, _ = http("DELETE", f"/estudos/{estudo_id}/convites/{conv_cancelar['id']}", joao)
    step("DELETE convite Ana", code, _)

    code, _ = http("GET", f"/estudos/{estudo_id}/convites", joao)
    step("GET convites do estudo", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/convites?status=pendente", joao)
    step("GET convites pendentes", code, _)
    code, _ = http("GET", "/convites", lucas)
    step("GET /convites recebidos (Lucas)", code, _)
    code, _ = http("GET", f"/convites/{conv_pendente['token']}")
    step("GET /convites/:token (publico)", code, _)

    # ── [6] Colaboradores ─────────────────────────────────────────────────────
    print("\n[6] Colaboradores")
    code, colabs = http("GET", f"/estudos/{estudo_id}/colaboradores", joao)
    step("GET colaboradores", code, colabs)
    maria_uid = next((col["id_usuario"] for col in colabs if col["email"] == "maria.colab@example.com"), None)
    if maria_uid:
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{maria_uid}", joao,
                       {"perfil": "proprietario"})
        step("PATCH Maria -> proprietario", code, _)
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{maria_uid}", joao,
                       {"perfil": "colaborador"})
        step("PATCH Maria -> colaborador (volta)", code, _)

    # ── [7] Codigo de acesso ──────────────────────────────────────────────────
    print("\n[7] Codigo de acesso")
    code, codigo = http("GET", f"/estudos/{estudo_id}/codigo_acesso", joao)
    step("GET codigo acesso", code, codigo)
    nova_senha = "AcessoSeed2026"
    code, _ = http("PATCH", f"/estudos/{estudo_id}/codigo_acesso", joao,
                   {"senha_autocadastro": nova_senha})
    step("PATCH senha_autocadastro", code, _)
    code, _ = http("POST", "/estudos/ingressar", beatriz,
                   {"codigo": codigo["codigo"], "senha_autocadastro": nova_senha})
    step("POST /estudos/ingressar (Beatriz)", code, _)

    # ── [8] Estudos secundarios ───────────────────────────────────────────────
    print("\n[8] Estudos secundarios")
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
    step("POST convite Joao -> Estudo 2 (pendente)", code, conv_joao)

    for token_dono, nome_estudo in [
        (maria,   "Caatinga - Reserva Serra das Almas"),
        (beatriz, "Pampa - Estancia do Sul"),
        (pedro,   "Pantanal - Rio Negro"),
    ]:
        code, est_extra = http("POST", "/estudos", token_dono, {
            "nome": nome_estudo,
            "observacoes": "Estudo auxiliar com convite pendente para Joao",
            "variaveis": [{"nome": "Observacao", "nivel_aplicacao": "campanha", "tipo_dado": "string"}],
        })
        step(f"POST /estudos ({nome_estudo})", code, est_extra)
        code, _ = http("POST", f"/estudos/{est_extra['id']}/convites", token_dono,
                       {"email_convidado": DONO_PRINCIPAL["email"]})
        step(f"POST convite Joao -> {nome_estudo}", code, _)

    code, _ = http("GET", "/convites", joao)
    step("GET /convites recebidos (Joao)", code, _)

    # ── [9] Listagens e filtros ───────────────────────────────────────────────
    print("\n[9] Listagens e filtros")
    code, _ = http("GET", "/estudos", joao)
    step("GET /estudos (Joao)", code, _)
    code, _ = http("GET", "/estudos?nome=Mata", joao)
    step("GET /estudos?nome=Mata", code, _)
    hoje = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    code, _ = http("GET", f"/estudos?criado_a_partir_de=2026-01-01&criado_ate={hoje}", joao)
    step("GET /estudos com filtros de data", code, _)

    # ── [10] Dashboard ────────────────────────────────────────────────────────
    print("\n[10] Dashboard")
    code, _ = http("GET", "/dashboard", joao)
    step("GET /dashboard (Joao)", code, _)
    code, _ = http("GET", "/dashboard", maria)
    step("GET /dashboard (Maria)", code, _)

    # ── [11] Exportacao ───────────────────────────────────────────────────────
    print("\n[11] Exportacao")
    for agrup in ["registro_ocorrencia", "evento_amostragem", "unidade_amostral", "campanha", "especie"]:
        code, _ = http("GET", f"/estudos/{estudo_id}/exportar_dados?formato=csv&agrupamento={agrup}", joao)
        step(f"exportar_dados csv agrupamento={agrup}", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/exportar_dados?formato=xml", joao)
    step("exportar_dados xml", code, _)

    # ── [12] Soft deletes ─────────────────────────────────────────────────────
    print("\n[12] Soft deletes")
    # Deleta especie[4] (Abelha-urucu) — nao afeta singletons/doubletons
    code, _ = http("DELETE", f"/estudos/{estudo_id}/especies/{especies_ids[4]}", joao)
    step("DELETE especie[4] Abelha-urucu", code, _)
    # Deleta ultimo evento (ultima campanha, ultima unidade, ultimo evento)
    last_c = len(campanhas) - 1
    last_u = len(LOCAIS) - 1
    ev_last = evento_map[(last_c, last_u, 2)]
    code, _ = http("DELETE",
        f"/estudos/{estudo_id}/campanhas/{ev_last['_campanha_id']}/unidades_amostrais/{ev_last['_unidade_id']}/eventos_amostragem/{ev_last['id']}",
        joao)
    step(f"DELETE evento[{last_c}][{last_u}][2]", code, _)
    # Deleta ultima unidade (ultima campanha, ultima localizacao)
    ua_last = unidade_map[(last_c, last_u)]
    code, _ = http("DELETE",
        f"/estudos/{estudo_id}/campanhas/{ua_last['_campanha_id']}/unidades_amostrais/{ua_last['id']}",
        joao)
    step(f"DELETE unidade[{last_c}][{last_u}]", code, _)

    # ── [13] Resumo ───────────────────────────────────────────────────────────
    print("\n[13] Resumo")
    code, _ = http("GET", "/dashboard", joao)
    step("GET /dashboard final", code, _)
    code, lista_final = http("GET", "/estudos", joao)
    step("GET /estudos final", code, lista_final)

    n_eventos  = len(LOCAIS) * len(campanhas) * 3
    n_anos     = len({cd[1][:4] for cd in CAMPANHAS_DEF})
    print("\n== Concluido ==")
    print(f"Login: email={DONO_PRINCIPAL['email']}  senha={DONO_PRINCIPAL['senha']}")
    print(f"Estudo principal id={estudo_id}  '{est1['nome']}'")
    print(f"Estudo secundario id={estudo2_id} '{est2['nome']}'")
    print(f"Anos cobertos: {n_anos} | Campanhas: {len(campanhas)} | Localizacoes: {len(LOCAIS)}")
    print(f"Especies: {len(especies_ids)} | Unidades: {len(unidade_map)} | "
          f"Eventos: {n_eventos} | Registros: {len(registros)}")
    print("Singletons Chao1: sp[9] e sp[10] (1 ocorrencia cada)")
    print("Doubleton  Chao2: sp[11] (2 unidades distintas, qtde=1 cada)")
    print(f"Convites pendentes para {DONO_PRINCIPAL['email']}: 4")
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
