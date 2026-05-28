#!/usr/bin/env python3
"""
Popula a API Kheprix de forma organica via HTTP.

Estudo: Entomofauna do Jardim Botanico de Curitiba (PR)
  Bioma: Floresta Ombrofila Mista (Araucaria)
  Local: Jardim Botanico Francisca Maria Garfunkel Richbieter

Dados dimensionados para cobrir todas as 38 analises disponiveis na API R:
  - 12 especies (incl. singletons e doubletons para Chao1/Chao2)
  - 9 campanhas: 3 anos (2024/2025/2026) x 3 estacoes (verao/outono/inverno)
  - 3 grupos sazonais para ANOVA; series temporais para analises interanuais
  - 4 unidades por campanha = 36 totais (matrizes, nMDS, RDA/CCA)
  - 3 eventos por unidade = 108 eventos
  - ~650 registros de ocorrencia cobrindo 4 zonas do Jardim Botanico
  - Variaveis em todos os niveis: campanha(1), unidade(3), evento(3), registro(1)

Uso:
    python seed_organica2.py [--base-url http://localhost:3000]

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
import time
import zlib
from datetime import datetime, timezone
from typing import Any
from urllib import error as urlerror
from urllib import request as urlreq


BASE_URL = "http://localhost:3000"

DONO_PRINCIPAL = {"nome": "Keps", "email": "kheprixapp@gmail.com", "senha": "senha123"}

OUTROS_USUARIOS = [
    {"nome": "Sofia Colaboradora", "email": "sofia.colab2@example.com",   "senha": "senha123"},
    {"nome": "Tomas Recusa",       "email": "tomas.recusa2@example.com",  "senha": "senha123"},
    {"nome": "Camila Cancelada",   "email": "camila.cancel2@example.com", "senha": "senha123"},
    {"nome": "Diego Pendente",     "email": "diego.pend2@example.com",    "senha": "senha123"},
    {"nome": "Larissa Codigo",     "email": "larissa.cod2@example.com",   "senha": "senha123"},
    {"nome": "Felipe Outro",       "email": "felipe.outro2@example.com",  "senha": "senha123"},
]


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
    last_exc: Exception | None = None
    for attempt in range(3):
        try:
            req = urlreq.Request(url, headers={"User-Agent": "KheprixSeed/1.0 (educational)"})
            with urlreq.urlopen(req, timeout=15) as resp:
                data = resp.read()
                ct = resp.headers.get("Content-Type", "image/jpeg").split(";")[0].strip()
                if not ct.startswith("image/"):
                    ct = "image/jpeg"
                result = f"data:{ct};base64," + base64.b64encode(data).decode()
                _img_cache[url] = result
                return result
        except urlerror.HTTPError as exc:
            last_exc = exc
            if exc.code == 429 and attempt < 2:
                wait = 5 * (attempt + 1)
                print(f"    [WAIT] rate-limit ({url[:60]}), aguardando {wait}s...")
                time.sleep(wait)
            else:
                break
        except Exception as exc:
            last_exc = exc
            break
    print(f"    [WARN] download falhou ({url[:70]}): {last_exc}")
    result = png_b64(*fallback)
    _img_cache[url] = result
    return result


# (url, cor_fallback_rgb) — fotos via Wikipedia Commons
# URLs verificadas: sp[4] e sp[5] confirmadas; demais usam nomes canônicos do Commons
_ESPECIES_IMG: list[tuple[str, tuple[int, int, int]]] = [
    # Acromyrmex octospinosus — proxy para Formiga-cortadeira (nome canônico no Commons)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Acromyrmex_octospinosus.jpg?width=400",         (120,  80,  40)),
    # Bombus terrestris — proxy para Mamangava (espécie com farto acervo no Commons)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Bombus_terrestris.jpg?width=400",               ( 20,  20,  20)),
    # Heliconius erato cyrbia — proxy para Borboleta-imperatriz (confirmado no original)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Heliconius_erato_cyrbia_1.jpg?width=400",       ( 80,  60, 160)),
    # Erythrodiplax fusca — proxy para Libélula-azul (confirmado no original)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Erythrodiplax_fusca.jpg?width=400",             ( 60, 100, 200)),
    # Apis mellifera — Abelha-europeia (confirmado)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Apis_mellifera_flying.jpg?width=400",           (220, 180,  40)),
    # Vanessa braziliensis — Pinta-perna (confirmado)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Vanessa_braziliensis.jpg?width=400",            (200, 100,  30)),
    # Melolontha melolontha — proxy para Corô-dos-jardins (verificado OK)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Melolontha_melolontha.jpg?width=400",           (140, 100,  60)),
    # Dichotomius species — proxy para Besouro-esterqueiro (confirmado no original)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Scarabaeidae_-_Dichotomius_species.JPG?width=400", ( 20,  20,  80)),
    # Gryllus assimilis — proxy para Grilo-tropical (confirmado no original)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Gryllus_assimilis.jpg?width=400",               ( 80, 140,  80)),
    # Forficula auricularia — Tesourinha (singleton A), sem sufixo _male
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Forficula_auricularia.jpg?width=400",           (100, 160, 100)),
    # Empoasca kraemeri — proxy para Cigarrinha-da-cana (singleton B, confirmado no original)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Empoasca_kraemeri.jpg?width=400",               (200, 200, 100)),
    # Cicada orni — proxy para Cigarra-verde (Chao2 Q2, bem documentada no Commons)
    ("https://commons.wikimedia.org/wiki/Special:FilePath/Cicada_orni.jpg?width=400",                     ( 60, 160,  60)),
]



# 9 campanhas: 3 anos x 3 estacoes | clima subtropical de Curitiba
# (nome, dt_ini, dt_fim, responsavel, [(yr,mo,dy)x3], temp_base, precip_base, humid_base)
CAMPANHAS_DEF = [
    ("Campanha Verao 2024",   "2024-01-08", "2024-03-29", "Rafaela",
     [(2024, 1,  8), (2024, 2,  6), (2024, 3,  5)], 24.0, 180.0, 76.0),
    ("Campanha Outono 2024",  "2024-04-03", "2024-06-27", "Sofia",
     [(2024, 4,  3), (2024, 5,  8), (2024, 6,  5)], 17.5,  98.0, 71.0),
    ("Campanha Inverno 2024", "2024-07-02", "2024-09-26", "Larissa",
     [(2024, 7,  2), (2024, 8, 13), (2024, 9,  4)], 12.5,  72.0, 68.0),
    ("Campanha Verao 2025",   "2025-01-10", "2025-03-31", "Rafaela",
     [(2025, 1, 10), (2025, 2,  5), (2025, 3,  7)], 24.5, 185.0, 77.0),
    ("Campanha Outono 2025",  "2025-04-05", "2025-06-28", "Sofia",
     [(2025, 4,  5), (2025, 5, 10), (2025, 6,  9)], 17.8, 100.0, 71.5),
    ("Campanha Inverno 2025", "2025-07-04", "2025-09-28", "Larissa",
     [(2025, 7,  4), (2025, 8, 12), (2025, 9,  2)], 12.8,  75.0, 68.5),
    ("Campanha Verao 2026",   "2026-01-12", "2026-03-30", "Rafaela",
     [(2026, 1, 12), (2026, 2,  9), (2026, 3,  4)], 25.0, 190.0, 78.0),
    ("Campanha Outono 2026",  "2026-04-03", "2026-06-29", "Sofia",
     [(2026, 4,  3), (2026, 5,  8), (2026, 6,  7)], 18.0, 102.0, 72.0),
    ("Campanha Inverno 2026", "2026-07-03", "2026-09-27", "Larissa",
     [(2026, 7,  3), (2026, 8, 11), (2026, 9,  3)], 13.0,  78.0, 69.0),
]

# 4 unidades amostrais dentro do Jardim Botanico de Curitiba (alt ~924 m)
# (nome, lat, lon, raio, metodo, esforco, solo, altitude, cobertura_vegetal)
LOCAIS = [
    ("JB-01-Estufa",      -25.4483, -49.2327, 50.0, "Armadilha fotografica", "30 dias de exposicao",  "Latossolo vermelho", 924.0, 85.0),
    ("JB-02-Lago",        -25.4492, -49.2338, 60.0, "Busca ativa",           "8h/dia por 5 dias",     "Hidromorfico",       920.0, 70.0),
    ("JB-03-Mata-Ciliar", -25.4498, -49.2320, 70.0, "Pitfall",               "10 armadilhas/30 dias", "Organico-humoso",    922.0, 92.0),
    ("JB-04-Canteiros",   -25.4478, -49.2342, 45.0, "Malaise",               "15 dias continuo",      "Argiloso",           926.0, 60.0),
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
    print(f"== Kheprix seed organica2 - Jardim Botanico de Curitiba em {BASE_URL} ==\n")

    print("[0] Baixando imagens reais ...")
    fotos_especies = [imagem_b64(url, fb) for url, fb in _ESPECIES_IMG]
    print(f"  {len(fotos_especies)} fotos de especies prontas\n")

    print("[1] Criando usuarios e fazendo login")
    tokens: dict[str, str] = {}
    tokens[DONO_PRINCIPAL["email"]] = criar_ou_logar(DONO_PRINCIPAL)
    for u in OUTROS_USUARIOS:
        tokens[u["email"]] = criar_ou_logar(u)
    rafaela = tokens[DONO_PRINCIPAL["email"]]
    sofia   = tokens["sofia.colab2@example.com"]
    tomas   = tokens["tomas.recusa2@example.com"]
    diego   = tokens["diego.pend2@example.com"]
    larissa = tokens["larissa.cod2@example.com"]
    felipe  = tokens["felipe.outro2@example.com"]

    print("\n[2] Estudo principal - Jardim Botanico de Curitiba")
    code, est1 = http("POST", "/estudos", rafaela, {
        "nome": "Entomofauna - Jardim Botanico de Curitiba",
        "observacoes": "Monitoramento de insetos no bioma de Araucaria - Curitiba PR",
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
    step("POST /estudos (Estudo JB)", code, est1)
    estudo_id = est1["id"]

    code, variaveis = http("GET", f"/estudos/{estudo_id}/variaveis", rafaela)
    step("GET /variaveis", code, variaveis)
    vbn               = {v["nome"]: v["id"] for v in variaveis}
    var_responsavel   = vbn["Responsavel"]
    var_solo          = vbn["Tipo de solo"]
    var_altitude      = vbn["Altitude"]
    var_cobertura     = vbn["Cobertura vegetal"]
    var_temperatura   = vbn["Temperatura"]
    var_precipitacao  = vbn["Precipitacao"]
    var_umidade       = vbn["Umidade"]
    var_comportamento = vbn["Comportamento"]

    print("\n[3] Cadastrando 12 especies com foto")
    especies_payload = [
        {"classe": "Insecta", "ordem": "Hymenoptera", "familia": "Formicidae",     "genero": "Acromyrmex", "especie": "lundii",       "nome_popular": "Formiga-cortadeira-de-sete-espinhos", "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Hymenoptera", "familia": "Apidae",         "genero": "Bombus",     "especie": "morio",        "nome_popular": "Mamangava-preta",                     "status_conservacao": "Vulneravel",          "endemismo": True},
        {"classe": "Insecta", "ordem": "Lepidoptera", "familia": "Nymphalidae",    "genero": "Doxocopa",   "especie": "laurentia",    "nome_popular": "Borboleta-imperatriz",                "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Odonata",     "familia": "Coenagrionidae", "genero": "Argia",      "especie": "translata",    "nome_popular": "Libelula-azul-translucida",           "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Hymenoptera", "familia": "Apidae",         "genero": "Apis",       "especie": "mellifera",    "nome_popular": "Abelha-europeia",                     "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Lepidoptera", "familia": "Nymphalidae",    "genero": "Vanessa",    "especie": "braziliensis", "nome_popular": "Pinta-perna",                         "status_conservacao": "Pouco preocupante",   "endemismo": True},
        {"classe": "Insecta", "ordem": "Coleoptera",  "familia": "Scarabaeidae",   "genero": "Phyllophaga","especie": "cuyabana",     "nome_popular": "Coro-dos-jardins",                    "status_conservacao": "Dados insuficientes", "endemismo": False},
        {"classe": "Insecta", "ordem": "Coleoptera",  "familia": "Scarabaeidae",   "genero": "Copris",     "especie": "lunaris",      "nome_popular": "Besouro-esterqueiro",                 "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Orthoptera",  "familia": "Gryllidae",      "genero": "Gryllodes",  "especie": "sigillatus",   "nome_popular": "Grilo-tropical",                      "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Dermaptera",  "familia": "Forficulidae",   "genero": "Forficula",  "especie": "auricularia",  "nome_popular": "Tesourinha",                          "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Hemiptera",   "familia": "Cercopidae",     "genero": "Mahanarva",  "especie": "fimbriolata",  "nome_popular": "Cigarrinha-da-cana",                  "status_conservacao": "Pouco preocupante",   "endemismo": False},
        {"classe": "Insecta", "ordem": "Hemiptera",   "familia": "Cicadidae",      "genero": "Fidicina",   "especie": "mannifera",    "nome_popular": "Cigarra-verde",                       "status_conservacao": "Dados insuficientes", "endemismo": False},
    ]
    especies_ids: list[int] = []
    for sp_idx, sp in enumerate(especies_payload):
        code, resp = http("POST", f"/estudos/{estudo_id}/especies", rafaela,
                          {**sp, "foto": fotos_especies[sp_idx]})
        step(f"POST especie {sp['nome_popular']}", code, resp)
        especies_ids.append(resp["id"])

    code, _ = http("GET", f"/estudos/{estudo_id}/especies", rafaela)
    step("GET /especies", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/especies/{especies_ids[0]}", rafaela)
    step("GET /especies/:id", code, _)
    code, _ = http("PATCH", f"/estudos/{estudo_id}/especies/{especies_ids[1]}", rafaela,
                   {"nome_popular": "Mamangava-preta (Araucaria)"})
    step("PATCH especie[1]", code, _)

    print("\n[4] Campanhas, unidades, eventos e registros")
    campanhas: list[dict] = []
    for camp_def in CAMPANHAS_DEF:
        camp_nome, dt_ini, dt_fim, resp_nome = camp_def[0], camp_def[1], camp_def[2], camp_def[3]
        code, camp = http("POST", f"/estudos/{estudo_id}/campanhas", rafaela, {
            "nome":        camp_nome,
            "data_inicio": dt_ini,
            "data_fim":    dt_fim,
            "descricao":   f"{camp_nome} - Jardim Botanico de Curitiba",
            "valores_variaveis": [{"variavel_id": var_responsavel, "valor": resp_nome}],
        })
        step(f"POST campanha {camp_nome}", code, camp)
        campanhas.append(camp)

    vv0_id = ((campanhas[0].get("valores_variaveis") or [{}])[0]).get("id")
    code, _ = http("PATCH", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", rafaela, {
        "nome":        campanhas[0]["nome"] + " (revisada)",
        "data_inicio": campanhas[0]["data_inicio"],
        "data_fim":    campanhas[0].get("data_fim"),
        "descricao":   "Descricao atualizada apos revisao de campo",
        "valores_variaveis": [{"id": vv0_id, "valor": "Rafaela (revisada)"}] if vv0_id else [],
    })
    step("PATCH campanha[0]", code, _)

    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas", rafaela)
    step("GET campanhas", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/campanhas/{campanhas[0]['id']}", rafaela)
    step("GET campanha :id", code, _)

    unidade_map: dict[tuple[int, int], dict] = {}
    for c_idx, campanha in enumerate(campanhas):
        estacao_ano = CAMPANHAS_DEF[c_idx][0].replace("Campanha ", "")
        for u_idx, local in enumerate(LOCAIS):
            nome, lat, lon, raio, metodo, esforco, solo, alt, cob = local
            code, ua = http("POST",
                f"/estudos/{estudo_id}/campanhas/{campanha['id']}/unidades_amostrais",
                rafaela, {
                    "nome":             f"{nome} {estacao_ano}",
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

    ua00 = unidade_map[(0, 0)]
    vvs_ua00 = {vv["variavel_id"]: vv["id"] for vv in (ua00.get("valores_variaveis") or [])}
    patch_ua_vv = []
    for var_id, new_val in [
        (var_solo,      "Latossolo vermelho (revisado)"),
        (var_altitude,  "925.0"),
        (var_cobertura, "87.0"),
    ]:
        if vvs_ua00.get(var_id):
            patch_ua_vv.append({"id": vvs_ua00[var_id], "valor": new_val})
    code, _ = http("PATCH",
        f"/estudos/{estudo_id}/campanhas/{ua00['_campanha_id']}/unidades_amostrais/{ua00['id']}",
        rafaela, {
            "nome":      ua00["nome"] + " (revisada)",
            "latitude":  ua00["latitude"],
            "longitude": ua00["longitude"],
            "raio":      55.0,
            "valores_variaveis": patch_ua_vv,
        })
    step("PATCH unidade[0][0]", code, _)

    evento_map: dict[tuple[int, int, int], dict] = {}
    for c_idx, (campanha, camp_def) in enumerate(zip(campanhas, CAMPANHAS_DEF)):
        datas_ev    = camp_def[4]
        base_temp   = camp_def[5]
        base_precip = camp_def[6]
        base_humid  = camp_def[7]
        for u_idx in range(len(LOCAIS)):
            ua = unidade_map[(c_idx, u_idx)]
            for e_idx, (yr, mo, dy) in enumerate(datas_ev):
                temp   = round(base_temp   + u_idx * 0.3 - e_idx * 0.4, 1)
                precip = round(base_precip + u_idx * 2.5 - e_idx * 7.0, 1)
                humid  = round(base_humid  + u_idx * 0.6 - e_idx * 1.2, 1)
                inicio = datetime(yr, mo, dy,  8, 0, 0, tzinfo=timezone.utc)
                fim    = datetime(yr, mo, dy, 12, 0, 0, tzinfo=timezone.utc)
                code, ev = http("POST",
                    f"/estudos/{estudo_id}/campanhas/{campanha['id']}/unidades_amostrais/{ua['id']}/eventos_amostragem",
                    rafaela, {
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

    ev000 = evento_map[(0, 0, 0)]
    vvs_ev000 = {vv["variavel_id"]: vv["id"] for vv in (ev000.get("valores_variaveis") or [])}
    patch_ev_vv = []
    for var_id, new_val in [
        (var_temperatura,  "26.3"),
        (var_precipitacao, "195.0"),
        (var_umidade,      "82.0"),
    ]:
        if vvs_ev000.get(var_id):
            patch_ev_vv.append({"id": vvs_ev000[var_id], "valor": new_val})
    code, _ = http("PATCH",
        f"/estudos/{estudo_id}/campanhas/{ev000['_campanha_id']}/unidades_amostrais/{ev000['_unidade_id']}/eventos_amostragem/{ev000['id']}",
        rafaela, {
            "horario_inicio": ev000["horario_inicio"],
            "horario_fim":    ev000.get("horario_fim"),
            "esforco_real":   "5h (re-medido apos calibracao)",
            "valores_variaveis": patch_ev_vv,
        })
    step("PATCH evento[0][0][0]", code, _)

    registros: list[dict] = []

    for c_idx in range(len(campanhas)):
        camp_def = CAMPANHAS_DEF[c_idx]
        datas_ev = camp_def[4]
        for u_idx in range(len(LOCAIS)):
            _, lat, lon = LOCAIS[u_idx][0], LOCAIS[u_idx][1], LOCAIS[u_idx][2]
            for e_idx in range(3):
                ev      = evento_map[(c_idx, u_idx, e_idx)]
                camp_id = ev["_campanha_id"]
                unit_id = ev["_unidade_id"]
                ev_id   = ev["id"]
                yr, mo, dy = datas_ev[e_idx]
                data_str = f"{yr}-{mo:02d}-{dy:02d}"

                def post_reg(sp_id: int, qtde: int, comp: str, ausencia: bool = False, foto: str = "") -> dict:
                    body = {
                        "especie_id":       sp_id,
                        "data":             data_str,
                        "hora":             "09:00:00",
                        "latitude":         lat + 0.0001,
                        "longitude":        lon + 0.0001,
                        "qtde_individuos":  qtde,
                        "ausencia_especie": ausencia,
                        "foto":             foto,
                        "valores_variaveis": [
                            {"variavel_id": var_comportamento, "valor": comp},
                        ],
                    }
                    code2, r = http("POST",
                        f"/estudos/{estudo_id}/campanhas/{camp_id}/unidades_amostrais/{unit_id}/eventos_amostragem/{ev_id}/registro_ocorrencias",
                        rafaela, body)
                    step(f"POST reg c{c_idx}u{u_idx}e{e_idx} sp_id={sp_id}", code2, r)
                    if isinstance(r, dict):
                        r["_path"] = (camp_id, unit_id, ev_id)
                        registros.append(r)
                        return r
                    return {}

                for sp_idx in range(9):
                    if (u_idx * 7 + e_idx * 3 + c_idx * 11 + sp_idx * 13) % 3 != 0:
                        qtde = ((c_idx + u_idx * 2 + e_idx + sp_idx * 3) % 5) + 2
                        comp = "true" if (sp_idx + e_idx) % 2 == 0 else "false"
                        post_reg(especies_ids[sp_idx], qtde, comp, foto=fotos_especies[sp_idx])

                if c_idx == 0 and u_idx == 0 and e_idx == 0:
                    post_reg(especies_ids[9],  1, "true",  foto=fotos_especies[9])   # Tesourinha (singleton A)
                if c_idx == 0 and u_idx == 1 and e_idx == 0:
                    post_reg(especies_ids[10], 1, "false", foto=fotos_especies[10])  # Cigarrinha-da-cana (singleton B)
                if (c_idx == 0 and u_idx == 0 and e_idx == 2) or \
                   (c_idx == 0 and u_idx == 2 and e_idx == 0):
                    post_reg(especies_ids[11], 1, "true",  foto=fotos_especies[11])  # Cigarra-verde (doubleton Chao2)

    if registros:
        reg0 = registros[0]
        vv_id0 = ((reg0.get("valores_variaveis") or [{}])[0]).get("id")
        code, _ = http("PATCH",
            f"/estudos/{estudo_id}/campanhas/{reg0['_path'][0]}/unidades_amostrais/{reg0['_path'][1]}/eventos_amostragem/{reg0['_path'][2]}/registro_ocorrencias/{reg0['id']}",
            rafaela, {
                "especie_id":       reg0["especie_id"],
                "data":             reg0["data"],
                "hora":             reg0["hora"],
                "latitude":         reg0["latitude"],
                "longitude":        reg0["longitude"],
                "qtde_individuos":  8,
                "ausencia_especie": False,
                "valores_variaveis": [{"id": vv_id0, "valor": "false"}] if vv_id0 else [],
            })
        step("PATCH registro[0]", code, _)

    if len(registros) >= 2:
        reg_last = registros[-1]
        code, _ = http("DELETE",
            f"/estudos/{estudo_id}/campanhas/{reg_last['_path'][0]}/unidades_amostrais/{reg_last['_path'][1]}/eventos_amostragem/{reg_last['_path'][2]}/registro_ocorrencias/{reg_last['id']}",
            rafaela)
        step("DELETE registro[-1] (soft delete)", code, _)

    ev_s = evento_map[(0, 0, 0)]
    code, _ = http("GET",
        f"/estudos/{estudo_id}/campanhas/{ev_s['_campanha_id']}/unidades_amostrais/{ev_s['_unidade_id']}/eventos_amostragem/{ev_s['id']}/registro_ocorrencias",
        rafaela)
    step("GET registros (lista)", code, _)

    print("\n[5] Convites com todos os status")
    code, conv_aceito = http("POST", f"/estudos/{estudo_id}/convites", rafaela,
                             {"email_convidado": "sofia.colab2@example.com"})
    step("POST convite Sofia (sera aceito)", code, conv_aceito)
    code, _ = http("POST", f"/convites/{conv_aceito['token']}/aceitar", sofia)
    step("POST aceitar convite Sofia", code, _)

    code, conv_recusado = http("POST", f"/estudos/{estudo_id}/convites", rafaela,
                               {"email_convidado": "tomas.recusa2@example.com"})
    step("POST convite Tomas (sera recusado)", code, conv_recusado)
    code, _ = http("POST", f"/convites/{conv_recusado['token']}/recusar", tomas)
    step("POST recusar convite Tomas", code, _)

    code, conv_pendente = http("POST", f"/estudos/{estudo_id}/convites", rafaela,
                               {"email_convidado": "diego.pend2@example.com"})
    step("POST convite Diego (fica pendente)", code, conv_pendente)

    code, conv_cancelar = http("POST", f"/estudos/{estudo_id}/convites", rafaela,
                               {"email_convidado": "camila.cancel2@example.com"})
    step("POST convite Camila (sera cancelado)", code, conv_cancelar)
    code, _ = http("DELETE", f"/estudos/{estudo_id}/convites/{conv_cancelar['id']}", rafaela)
    step("DELETE convite Camila", code, _)

    code, _ = http("GET", f"/estudos/{estudo_id}/convites", rafaela)
    step("GET convites do estudo", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/convites?status=pendente", rafaela)
    step("GET convites pendentes", code, _)
    code, _ = http("GET", "/convites", diego)
    step("GET /convites recebidos (Diego)", code, _)
    code, _ = http("GET", f"/convites/{conv_pendente['token']}")
    step("GET /convites/:token (publico)", code, _)

    print("\n[6] Colaboradores")
    code, colabs = http("GET", f"/estudos/{estudo_id}/colaboradores", rafaela)
    step("GET colaboradores", code, colabs)
    sofia_uid = next((col["id_usuario"] for col in colabs if col["email"] == "sofia.colab2@example.com"), None)
    if sofia_uid:
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{sofia_uid}", rafaela,
                       {"perfil": "proprietario"})
        step("PATCH Sofia -> proprietario", code, _)
        code, _ = http("PATCH", f"/estudos/{estudo_id}/colaboradores/{sofia_uid}", rafaela,
                       {"perfil": "colaborador"})
        step("PATCH Sofia -> colaborador (volta)", code, _)

    print("\n[7] Codigo de acesso")
    code, codigo = http("GET", f"/estudos/{estudo_id}/codigo_acesso", rafaela)
    step("GET codigo acesso", code, codigo)
    nova_senha = "AcessoJB2026"
    code, _ = http("PATCH", f"/estudos/{estudo_id}/codigo_acesso", rafaela,
                   {"senha_autocadastro": nova_senha})
    step("PATCH senha_autocadastro", code, _)
    code, _ = http("POST", "/estudos/ingressar", larissa,
                   {"codigo": codigo["codigo"], "senha_autocadastro": nova_senha})
    step("POST /estudos/ingressar (Larissa)", code, _)

    print("\n[8] Estudos secundarios")
    code, est2 = http("POST", "/estudos", felipe, {
        "nome": "Araucaria - Floresta Nacional de Irati",
        "observacoes": "Estudo paralelo para testar visao de colaborador",
        "variaveis": [
            {"nome": "Pluviosidade", "nivel_aplicacao": "campanha", "tipo_dado": "number", "metrica": "mm"},
            {"nome": "Habitat",      "nivel_aplicacao": "unidade",  "tipo_dado": "string"},
        ],
    })
    step("POST /estudos (Estudo 2 - Felipe)", code, est2)
    estudo2_id = est2["id"]
    code, conv_rafaela = http("POST", f"/estudos/{estudo2_id}/convites", felipe,
                              {"email_convidado": DONO_PRINCIPAL["email"]})
    step("POST convite Rafaela -> Estudo 2 (pendente)", code, conv_rafaela)

    for token_dono, nome_estudo in [
        (sofia,   "Mata Atlantica - Serra do Mar Paranaense"),
        (larissa, "Campos Gerais - Vila Velha"),
        (tomas,   "Pantanal Sul - Mato Grosso do Sul"),
    ]:
        code, est_extra = http("POST", "/estudos", token_dono, {
            "nome": nome_estudo,
            "observacoes": "Estudo auxiliar com convite pendente para Rafaela",
            "variaveis": [{"nome": "Observacao", "nivel_aplicacao": "campanha", "tipo_dado": "string"}],
        })
        step(f"POST /estudos ({nome_estudo})", code, est_extra)
        code, _ = http("POST", f"/estudos/{est_extra['id']}/convites", token_dono,
                       {"email_convidado": DONO_PRINCIPAL["email"]})
        step(f"POST convite Rafaela -> {nome_estudo}", code, _)

    code, _ = http("GET", "/convites", rafaela)
    step("GET /convites recebidos (Rafaela)", code, _)

    print("\n[9] Listagens e filtros")
    code, _ = http("GET", "/estudos", rafaela)
    step("GET /estudos (Rafaela)", code, _)
    code, _ = http("GET", "/estudos?nome=Botanico", rafaela)
    step("GET /estudos?nome=Botanico", code, _)
    hoje = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    code, _ = http("GET", f"/estudos?criado_a_partir_de=2026-01-01&criado_ate={hoje}", rafaela)
    step("GET /estudos com filtros de data", code, _)

    print("\n[10] Dashboard")
    code, _ = http("GET", "/dashboard", rafaela)
    step("GET /dashboard (Rafaela)", code, _)
    code, _ = http("GET", "/dashboard", sofia)
    step("GET /dashboard (Sofia)", code, _)

    print("\n[11] Exportacao")
    for agrup in ["registro_ocorrencia", "evento_amostragem", "unidade_amostral", "campanha", "especie"]:
        code, _ = http("GET", f"/estudos/{estudo_id}/exportar_dados?formato=csv&agrupamento={agrup}", rafaela)
        step(f"exportar_dados csv agrupamento={agrup}", code, _)
    code, _ = http("GET", f"/estudos/{estudo_id}/exportar_dados?formato=xml", rafaela)
    step("exportar_dados xml", code, _)

    print("\n[12] Soft deletes")
    code, _ = http("DELETE", f"/estudos/{estudo_id}/especies/{especies_ids[4]}", rafaela)
    step("DELETE especie[4] Abelha-europeia", code, _)
    last_c = len(campanhas) - 1
    last_u = len(LOCAIS) - 1
    ev_last = evento_map[(last_c, last_u, 2)]
    code, _ = http("DELETE",
        f"/estudos/{estudo_id}/campanhas/{ev_last['_campanha_id']}/unidades_amostrais/{ev_last['_unidade_id']}/eventos_amostragem/{ev_last['id']}",
        rafaela)
    step(f"DELETE evento[{last_c}][{last_u}][2]", code, _)
    ua_last = unidade_map[(last_c, last_u)]
    code, _ = http("DELETE",
        f"/estudos/{estudo_id}/campanhas/{ua_last['_campanha_id']}/unidades_amostrais/{ua_last['id']}",
        rafaela)
    step(f"DELETE unidade[{last_c}][{last_u}]", code, _)

    print("\n[13] Resumo")
    code, _ = http("GET", "/dashboard", rafaela)
    step("GET /dashboard final", code, _)
    code, lista_final = http("GET", "/estudos", rafaela)
    step("GET /estudos final", code, lista_final)

    n_eventos = len(LOCAIS) * len(campanhas) * 3
    n_anos    = len({cd[1][:4] for cd in CAMPANHAS_DEF})
    print("\n== Concluido ==")
    print(f"Login: email={DONO_PRINCIPAL['email']}  senha={DONO_PRINCIPAL['senha']}")
    print(f"Estudo principal id={estudo_id}  '{est1['nome']}'")
    print(f"Estudo secundario id={estudo2_id} '{est2['nome']}'")
    print(f"Anos cobertos: {n_anos} | Campanhas: {len(campanhas)} | Localizacoes: {len(LOCAIS)}")
    print(f"Especies: {len(especies_ids)} | Unidades: {len(unidade_map)} | "
          f"Eventos: {n_eventos} | Registros: {len(registros)}")
    print("Singletons Chao1: sp[9] Tesourinha e sp[10] Cigarrinha-da-cana (1 ocorrencia cada)")
    print("Doubleton  Chao2: sp[11] Cigarra-verde (2 unidades distintas, qtde=1 cada)")
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
