"""
Guard Rails de Entrada
======================
Valida a pergunta do usuário ANTES de enviar ao modelo.
Bloqueia prompt injection, perguntas fora do domínio e conteúdo abusivo.
"""
import re

from guards.base import GuardResult


# ---------------------------------------------------------------------------
# 1. Detecção de Prompt Injection
# Padrões tentam reescrever as instruções do modelo ou extrair informações
# internas do sistema.
# ---------------------------------------------------------------------------
_PROMPT_INJECTION = [
    r"ignore\s+(as\s+)?instru[çc][oõ]es\s+anteriores",
    r"esque[çc]a\s+(tudo|as\s+instru[çc][oõ]es)",
    r"voc[eê]\s+agora\s+[eé]\s+um",
    r"novo\s+papel",
    r"nova\s+personalidade",
    r"ignore\s+previous\s+instructions",
    r"forget\s+(everything|your\s+instructions)",
    r"you\s+are\s+now\s+a",
    r"act\s+as\s+(if\s+you\s+are|a\s+)",
    r"pretend\s+(you\s+are|to\s+be)",
    r"jailbreak",
    r"\bdan\s+mode\b",
    r"do\s+anything\s+now",
    r"system\s+prompt",
    r"mostre?\s+(o\s+)?(seu\s+)?prompt",
    r"revele?\s+(suas?\s+)?instru[çc][oõ]es",
    r"print\s+(your\s+)?(system\s+)?prompt",
    r"what\s+are\s+your\s+instructions",
    r"quais\s+s[aã]o\s+(suas\s+)?instru[çc][oõ]es",
    r"ignore\s+all\s+previous",
    r"disregard\s+(the\s+)?previous",
    # Tentativa de injetar SQL diretamente na pergunta
    r"\b(DROP|DELETE|INSERT|UPDATE|TRUNCATE)\s+(TABLE|FROM|INTO)\b",
    # Tentar exfiltrar variáveis de ambiente
    r"(api[_\s]?key|senha|password|secret|token)\s*(do\s+sistema|do\s+banco|interno)",
]

# ---------------------------------------------------------------------------
# 2. Relevância de Domínio
# O chatbot só responde sobre dados entomológicos. Perguntas longas sem
# nenhuma relação com o domínio são bloqueadas.
# ---------------------------------------------------------------------------
_KEYWORDS_DOMINIO = {
    # Taxonomia
    "espécie", "especie", "inseto", "artrópode", "artropode",
    "ordem", "família", "familia", "gênero", "genero", "classe",
    "nome científico", "nome cientifico", "taxonomia",
    # Coleta e estrutura
    "estudo", "campanha", "coleta", "registro", "amostragem",
    "unidade amostral", "armadilha", "evento", "amostra",
    # Métricas ecológicas
    "riqueza", "diversidade", "abundância", "abundancia", "ocorrência", "ocorrencia",
    "endemismo", "endêmica", "endemica", "nativa",
    # Conservação
    "ameaçada", "ameacada", "conservação", "conservacao",
    "iucn", "vulnerável", "vulneravel", "em perigo", "extinta",
    "status", "cr", "en", "vu",
    # Tempo e espaço
    "mês", "mes", "ano", "estação", "estacao", "verão", "inverno",
    "outono", "primavera", "latitude", "longitude", "localização", "localizacao",
    # Análise
    "dados", "análise", "analise", "variável", "variavel", "total",
    "quantidade", "distribuição", "distribuicao", "comparar", "média", "media",
}

# Palavras que indicam claramente fora do domínio (lista curta, intencional)
# ATENÇÃO: não incluir variáveis ecológicas (temperatura, umidade, pH, altitude,
# precipitação) — podem ser métricas legítimas de estudos de campo.
_KEYWORDS_FORA_DOMINIO = {
    # Culinária
    "receita", "culinária", "culinaria", "bolo", "pizza", "sopa", "cozinhar",
    # Entretenimento
    "futebol", "filme", "série", "serie", "novela", "música", "musica",
    "jogo", "videogame", "celebridade", "famoso",
    # Política e economia
    "política", "politica", "eleição", "eleicao", "presidente",
    "comprar", "vender", "preço", "preco", "salário", "salario",
    "investimento", "criptomoeda", "bitcoin",
    # Tecnologia não relacionada
    "programar", "javascript", "typescript", "html", "css",
    "hacker", "invadir", "malware", "vírus computador",
}


def _verificar_prompt_injection(pergunta: str) -> GuardResult:
    texto = pergunta.lower()
    for padrao in _PROMPT_INJECTION:
        if re.search(padrao, texto, re.IGNORECASE):
            return GuardResult(
                passou=False,
                motivo="Sua pergunta contém um padrão não permitido. Por favor, reformule."
            )
    return GuardResult(passou=True)


_MENSAGEM_FORA_DOMINIO = (
    "Só consigo responder perguntas sobre os dados de coleta entomológica "
    "registrados no Kheprix. Tente perguntar sobre espécies, campanhas, "
    "riqueza de espécies, abundância, etc."
)


def _verificar_relevancia(pergunta: str) -> GuardResult:
    texto = pergunta.lower()

    # Substring matching captura plurais e variações (estudo→estudos, espécie→espécies)
    tem_keyword_dominio = any(kw in texto for kw in _KEYWORDS_DOMINIO)
    tem_keyword_fora = any(kw in texto for kw in _KEYWORDS_FORA_DOMINIO)

    # Keyword explicitamente fora do domínio → bloqueia independente do tamanho
    if tem_keyword_fora and not tem_keyword_dominio:
        return GuardResult(passou=False, motivo=_MENSAGEM_FORA_DOMINIO)

    # Pergunta substantiva (≥ 5 palavras) sem nenhuma keyword do domínio → bloqueia
    if not tem_keyword_dominio and len(pergunta.split()) >= 5:
        return GuardResult(passou=False, motivo=_MENSAGEM_FORA_DOMINIO)

    return GuardResult(passou=True)


def _verificar_caracteres_suspeitos(pergunta: str) -> GuardResult:
    """Bloqueia strings com caracteres de controle ou encoding malicioso."""
    if re.search(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]", pergunta):
        return GuardResult(passou=False, motivo="Pergunta contém caracteres inválidos.")
    return GuardResult(passou=True)


# ---------------------------------------------------------------------------
# Pipeline de entrada — executa todas as verificações em ordem
# ---------------------------------------------------------------------------
_CHECAGENS = [
    _verificar_caracteres_suspeitos,
    _verificar_prompt_injection,
    _verificar_relevancia,
]


def validar_entrada(pergunta: str) -> GuardResult:
    """
    Executa o pipeline de guard rails de entrada.
    Retorna na primeira falha encontrada.
    """
    for checagem in _CHECAGENS:
        resultado = checagem(pergunta)
        if not resultado.passou:
            return resultado
    return GuardResult(passou=True)
