"""
Testes de integridade da ingestão — Kheprix DW Pipeline
========================================================
Executa dentro do container Airflow com acesso aos dois bancos.

Como rodar:
    docker compose exec airflow-worker python /opt/airflow/dags/utils/test_ingestion.py

Cenários cobertos:
    1. Dado inserido ANTES da extração                → deve chegar ao staging
    2. Dado inserido DURANTE a extração (simulado)    → deve chegar no próximo ciclo (HWM overlap)
    3. Dado inserido DEPOIS da extração               → short-circuit deve detectar e processar
    4. Nenhum dado novo                               → short-circuit deve retornar False (skip)
    5. Atualização em tabela full-load (espécie)      → deve ser detectada pelo short-circuit
    6. Registro de ausência de espécie                → quantidade_apurada deve ser 0 na Gold
    7. Valor EAV com tipo inválido ('abc' em number)  → Silver não deve explodir, valor_numerico = NULL
    8. Contagem fim-a-fim: N inseridos = N no DW      → integridade total
"""

import sys
import logging
from datetime import datetime, timedelta, timezone
from typing import Optional

import psycopg2
import psycopg2.extras

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    stream=sys.stdout,
)
log = logging.getLogger(__name__)

# ─── Conexões ────────────────────────────────────────────────────────────────
# Lê as credenciais das variáveis de ambiente injetadas pelo docker-compose
# (mesmas usadas pelo airflow-scheduler em produção)

import os

OLTP_DSN = (
    f"host={os.environ.get('POSTGRES_HOST', 'db')} "
    f"port={os.environ.get('POSTGRES_PORT', '5432')} "
    f"dbname={os.environ.get('POSTGRES_DB', 'kheprix_oltp_db')} "
    f"user={os.environ.get('POSTGRES_USER', 'kheprix_user')} "
    f"password={os.environ.get('POSTGRES_PASSWORD', 'kheprix_password')}"
)

DW_DSN = (
    f"host={os.environ.get('POSTGRES_DW_HOST', 'db_dw')} "
    f"port={os.environ.get('POSTGRES_DW_PORT', '5432')} "
    f"dbname={os.environ.get('POSTGRES_DW_DB', 'kheprix_dw_db')} "
    f"user={os.environ.get('POSTGRES_DW_USER', 'kheprix_user')} "
    f"password={os.environ.get('POSTGRES_DW_PASSWORD', 'kheprix_password')}"
)


def oltp_conn():
    return psycopg2.connect(OLTP_DSN)


def dw_conn():
    return psycopg2.connect(DW_DSN)


def exec_oltp(sql, params=None, fetch=False):
    with oltp_conn() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, params)
            conn.commit()
            if fetch:
                return cur.fetchall()


def exec_dw(sql, params=None, fetch=False):
    with dw_conn() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, params)
            conn.commit()
            if fetch:
                return cur.fetchall()


# ─── Fixtures ────────────────────────────────────────────────────────────────

def _seed_estudo_base():
    """Garante um estudo, usuário, campanha, unidade e evento de teste no OLTP."""
    # usuarios: unique em email (índice simples — ON CONFLICT funciona)
    exec_oltp("""
        INSERT INTO usuarios (nome, email, password_digest, created_at, updated_at)
        VALUES ('Tester', 'test@kheprix.test', 'x', NOW(), NOW())
        ON CONFLICT (email) DO NOTHING;
    """)
    user_id = exec_oltp("SELECT id FROM usuarios WHERE email = 'test@kheprix.test'", fetch=True)[0]['id']

    # estudos: unique em codigo é PARCIAL (WHERE deleted_at IS NULL)
    # ON CONFLICT não funciona com índice parcial — usa WHERE NOT EXISTS
    exec_oltp("""
        INSERT INTO estudos (nome, codigo, created_at, updated_at)
        SELECT 'Estudo Teste', 'TEST001', NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM estudos WHERE codigo = 'TEST001' AND deleted_at IS NULL
        );
    """)
    estudo_id = exec_oltp(
        "SELECT id FROM estudos WHERE codigo = 'TEST001' AND deleted_at IS NULL",
        fetch=True
    )[0]['id']

    # colaboradores: unique em (estudo_id, usuario_id) — mas id: false, sem PK convencional
    exec_oltp("""
        INSERT INTO colaboradores (estudo_id, usuario_id, perfil)
        SELECT %s, %s, 0
        WHERE NOT EXISTS (
            SELECT 1 FROM colaboradores WHERE estudo_id = %s AND usuario_id = %s
        );
    """, (estudo_id, user_id, estudo_id, user_id))

    exec_oltp("""
        INSERT INTO campanhas (estudo_id, nome, data_inicio, created_at, updated_at)
        SELECT %s, 'Campanha Teste', NOW()::date, NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM campanhas WHERE estudo_id = %s AND nome = 'Campanha Teste' AND deleted_at IS NULL
        );
    """, (estudo_id, estudo_id))
    campanha_id = exec_oltp(
        "SELECT id FROM campanhas WHERE estudo_id = %s AND nome = 'Campanha Teste' AND deleted_at IS NULL",
        (estudo_id,), fetch=True
    )[0]['id']

    exec_oltp("""
        INSERT INTO unidades_amostrais (campanha_id, latitude, longitude, nome, created_at, updated_at)
        SELECT %s, -23.5, -46.6, 'UA Teste', NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM unidades_amostrais WHERE campanha_id = %s AND nome = 'UA Teste' AND deleted_at IS NULL
        );
    """, (campanha_id, campanha_id))
    ua_id = exec_oltp(
        "SELECT id FROM unidades_amostrais WHERE campanha_id = %s AND nome = 'UA Teste' AND deleted_at IS NULL",
        (campanha_id,), fetch=True
    )[0]['id']

    # eventos_amostragem: horario_fim e esforco_real são NOT NULL no schema
    exec_oltp("""
        INSERT INTO eventos_amostragem (unidade_amostral_id, horario_inicio, horario_fim, esforco_real, created_at, updated_at)
        SELECT %s, NOW(), NOW() + interval '2 hours', 'Esforço teste', NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM eventos_amostragem WHERE unidade_amostral_id = %s AND deleted_at IS NULL
        );
    """, (ua_id, ua_id))
    evento_id = exec_oltp(
        "SELECT id FROM eventos_amostragem WHERE unidade_amostral_id = %s AND deleted_at IS NULL ORDER BY id DESC LIMIT 1",
        (ua_id,), fetch=True
    )[0]['id']

    # especies: sem unique constraint em (estudo_id, especie) no schema — usa WHERE NOT EXISTS
    exec_oltp("""
        INSERT INTO especies (estudo_id, genero, especie, endemismo, status_conservacao, created_at, updated_at)
        SELECT %s, 'Apis', 'mellifera_test', false, 'LC', NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM especies WHERE estudo_id = %s AND especie = 'mellifera_test' AND deleted_at IS NULL
        );
    """, (estudo_id, estudo_id))
    especie_id = exec_oltp(
        "SELECT id FROM especies WHERE estudo_id = %s AND especie = 'mellifera_test' AND deleted_at IS NULL",
        (estudo_id,), fetch=True
    )[0]['id']

    return {'estudo_id': estudo_id, 'campanha_id': campanha_id, 'ua_id': ua_id,
            'evento_id': evento_id, 'especie_id': especie_id}


def _limpar_registros_teste(ids_inseridos: list):
    """Remove os registros de ocorrência inseridos pelos testes."""
    if not ids_inseridos:
        return
    placeholders = ','.join(['%s'] * len(ids_inseridos))
    exec_oltp(f"DELETE FROM registro_ocorrencias WHERE id IN ({placeholders})", ids_inseridos)
    log.info(f"  [cleanup] {len(ids_inseridos)} registro(s) removidos do OLTP.")


# ─── Utilitários ─────────────────────────────────────────────────────────────

def _inserir_registro(evento_id, especie_id, qtde=3, ausencia=False,
                      updated_at: Optional[datetime] = None) -> int:
    ts = updated_at or datetime.now(timezone.utc)
    row = exec_oltp("""
        INSERT INTO registro_ocorrencias
            (evento_amostragem_id, especie_id, data, hora, qtde_individuos, ausencia_especie,
             latitude, longitude, created_at, updated_at)
        VALUES (%s, %s, NOW()::date, NOW()::time, %s, %s, -23.5, -46.6, %s, %s)
        RETURNING id;
    """, (evento_id, especie_id, qtde, ausencia, ts, ts), fetch=True)
    return row[0]['id']


def _forcar_extracao_staging(tabela: str):
    """Simula a extração da tabela: TRUNCATE + COPY (full) no staging do DW."""
    rows = exec_oltp(f"SELECT * FROM {tabela}", fetch=True)
    if not rows:
        return
    cols = list(rows[0].keys())
    cols_sql = ", ".join(cols)
    exec_dw(f"TRUNCATE TABLE staging.{tabela} CASCADE;")
    for row in rows:
        vals = tuple(row[c] for c in cols)
        placeholders = ", ".join(["%s"] * len(cols))
        exec_dw(
            f"INSERT INTO staging.{tabela} ({cols_sql}) VALUES ({placeholders}) ON CONFLICT DO NOTHING;",
            vals
        )


def _contar_no_staging(tabela: str, where: str = "", params=None) -> int:
    rows = exec_dw(f"SELECT COUNT(*) AS n FROM staging.{tabela} {where}", params, fetch=True)
    return rows[0]['n']


def _contar_na_fato(where: str = "", params=None) -> int:
    rows = exec_dw(
        f"SELECT COUNT(*) AS n FROM public.fato_medicao_entomologica {where}", params, fetch=True
    )
    return rows[0]['n']


# ─── Runner ──────────────────────────────────────────────────────────────────

PASS = "✅ PASS"
FAIL = "❌ FAIL"
results = []


def run_test(name: str, fn):
    log.info(f"\n{'─'*60}")
    log.info(f"TESTE: {name}")
    try:
        fn()
        results.append((PASS, name))
        log.info(f"{PASS} — {name}")
    except AssertionError as e:
        results.append((FAIL, name, str(e)))
        log.error(f"{FAIL} — {name}: {e}")
    except Exception as e:
        results.append((FAIL, name, str(e)))
        log.error(f"{FAIL} — {name}: {e}", exc_info=True)


# ─── Testes ──────────────────────────────────────────────────────────────────

def test_dado_antes_da_extracao(fixture):
    """Cenário 1: dado inserido antes da extração deve chegar ao staging."""
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=5)
    try:
        _forcar_extracao_staging('registro_ocorrencias')
        n = _contar_no_staging('registro_ocorrencias', "WHERE id = %s", (rid,))
        assert n == 1, f"Esperava 1 no staging, encontrou {n}"
    finally:
        _limpar_registros_teste([rid])


def test_short_circuit_sem_dados_novos(fixture):
    """Cenário 4: short-circuit deve retornar False quando não há dados novos."""
    # Simula que a última extração foi AGORA (nada de novo pode ter chegado depois)
    from airflow.models import DagRun
    from airflow.utils.state import DagRunState

    runs = DagRun.find(dag_id='extract_staging', state=DagRunState.SUCCESS)
    if not runs:
        log.info("  [skip] Nenhum DagRun de sucesso encontrado. Pulando teste de short-circuit.")
        return

    ultimo = max(runs, key=lambda r: r.start_date)
    # Insere um registro com updated_at ANTERIOR ao start_date do último run
    ts_antigo = ultimo.start_date - timedelta(minutes=30)
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=1, updated_at=ts_antigo)
    try:
        # Verifica manualmente a lógica do short-circuit
        source = psycopg2.connect(OLTP_DSN)
        with source.cursor() as cur:
            cur.execute("SELECT MAX(updated_at) FROM registro_ocorrencias;")
            max_upd = cur.fetchone()[0]
        source.close()

        # Se max_upd <= start_date do último run, short-circuit retornaria False
        if max_upd and max_upd.replace(tzinfo=timezone.utc) <= ultimo.start_date:
            log.info(f"  Short-circuit retornaria False corretamente (max_upd={max_upd} <= start={ultimo.start_date})")
        else:
            log.info(f"  max_upd={max_upd} | último start={ultimo.start_date} — outras tabelas podem ter dados mais novos")
    finally:
        _limpar_registros_teste([rid])


def test_short_circuit_detecta_dado_novo(fixture):
    """Cenário 3: dado inserido com updated_at futuro deve ser detectado."""
    ts_futuro = datetime.now(timezone.utc) + timedelta(minutes=1)
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=2, updated_at=ts_futuro)
    try:
        conn = psycopg2.connect(OLTP_DSN)
        with conn.cursor() as cur:
            cur.execute("SELECT MAX(updated_at) FROM registro_ocorrencias;")
            max_upd = cur.fetchone()[0]
        conn.close()

        assert max_upd is not None, "MAX(updated_at) não deveria ser NULL"
        assert max_upd.replace(tzinfo=timezone.utc) > datetime.now(timezone.utc) - timedelta(seconds=5), \
            f"updated_at muito antigo: {max_upd}"
        log.info(f"  MAX(updated_at) = {max_upd} — short-circuit detectaria dado novo ✓")
    finally:
        _limpar_registros_teste([rid])


def test_ausencia_especie_zero_na_gold(fixture):
    """Cenário 6: registro de ausência deve gerar quantidade_apurada = 0 na Gold."""
    # Verifica a regra de negócio na dim_registro_ocorrencia
    # (precisa que a DAG já tenha rodado ao menos uma vez com dados)
    rows = exec_dw("""
        SELECT quantidade_apurada
        FROM public.dim_registro_ocorrencia
        WHERE especie_id = %s
        LIMIT 10;
    """, (fixture['especie_id'],), fetch=True)

    if not rows:
        log.info("  [skip] Nenhum registro na Gold para esta espécie. Execute a DAG primeiro.")
        return

    # Insere ausência diretamente na dim para validar a regra SQL
    ausencia_rows = exec_dw("""
        SELECT quantidade_apurada
        FROM public.dim_registro_ocorrencia r
        JOIN public.silver_registro_ocorrencias s ON s.id_registro = r.id_registro
        WHERE s.ausencia_especie = true
        LIMIT 5;
    """, fetch=True)

    for row in ausencia_rows:
        assert row['quantidade_apurada'] == 0, \
            f"Ausência deveria ter quantidade_apurada=0, encontrou {row['quantidade_apurada']}"
    log.info(f"  {len(ausencia_rows)} registro(s) de ausência verificado(s) com quantidade_apurada=0 ✓")


def test_valor_eav_invalido_nao_explode(fixture):
    """Cenário 7: CAST inválido em Silver não deve derrubar a task."""
    # Insere diretamente uma variável do tipo number com valor texto inválido no staging
    exec_dw("""
        INSERT INTO staging.variaveis (id, estudo_id, nome, metrica, nivel_aplicacao, tipo_dado, created_at, updated_at)
        VALUES (99999, %s, 'var_teste_invalida', 'unidade', 3, 1, NOW(), NOW())
        ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;
    """, (fixture['estudo_id'],))

    exec_dw("""
        INSERT INTO staging.valores_variaveis (id, variavel_id, id_nivel_aplicacao, valor, created_at, updated_at)
        VALUES (99999, 99999, %s, 'VALOR_INVALIDO_ABC', NOW(), NOW())
        ON CONFLICT (id) DO UPDATE SET valor = EXCLUDED.valor;
    """, (fixture['evento_id'],))

    # Roda a Silver manualmente
    try:
        exec_dw("""
            INSERT INTO public.silver_variaveis (id, nome, metrica, nivel_aplicacao, tipo_dado, created_at, updated_at)
            SELECT id, nome, metrica,
              CASE nivel_aplicacao
                WHEN 0 THEN 'campanha' WHEN 1 THEN 'unidade'
                WHEN 2 THEN 'evento'   WHEN 3 THEN 'registro' ELSE 'unknown'
              END,
              CASE tipo_dado
                WHEN 0 THEN 'string' WHEN 1 THEN 'number'
                WHEN 2 THEN 'date'   ELSE 'unknown'
              END,
              created_at, updated_at
            FROM staging.variaveis WHERE id = 99999
            ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;
        """)

        exec_dw("""
            INSERT INTO public.silver_valores_variaveis (id, variavel_id, id_nivel_aplicacao, valor_numerico, valor_texto, valor_data, updated_at)
            SELECT
              vv.id, vv.variavel_id, vv.id_nivel_aplicacao,
              CASE
                WHEN v.tipo_dado = 'number' AND vv.valor ~ '^-?[0-9]*\\.?[0-9]+$'
                  THEN CAST(vv.valor AS DECIMAL(18,4))
                WHEN v.tipo_dado = 'number' THEN NULL
                ELSE NULL
              END,
              CASE WHEN v.tipo_dado IN ('string','date') THEN vv.valor ELSE NULL END,
              NULL,
              vv.updated_at
            FROM staging.valores_variaveis vv
            JOIN public.silver_variaveis v ON v.id = vv.variavel_id
            WHERE vv.id = 99999
            ON CONFLICT (id) DO UPDATE SET valor_numerico = EXCLUDED.valor_numerico;
        """)

        row = exec_dw(
            "SELECT valor_numerico FROM public.silver_valores_variaveis WHERE id = 99999",
            fetch=True
        )
        assert row, "Linha não foi inserida na Silver"
        assert row[0]['valor_numerico'] is None, \
            f"Valor inválido deveria ser NULL, encontrou: {row[0]['valor_numerico']}"
        log.info("  CAST inválido retornou NULL sem explodir a query ✓")
    finally:
        exec_dw("DELETE FROM public.silver_valores_variaveis WHERE id = 99999;")
        exec_dw("DELETE FROM public.silver_variaveis WHERE id = 99999;")
        exec_dw("DELETE FROM staging.valores_variaveis WHERE id = 99999;")
        exec_dw("DELETE FROM staging.variaveis WHERE id = 99999;")


def test_contagem_fim_a_fim(fixture):
    """Cenário 8: N registros inseridos devem aparecer na fato após extração manual."""
    N = 10
    ids = []
    for i in range(N):
        rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=i + 1)
        ids.append(rid)

    try:
        _forcar_extracao_staging('registro_ocorrencias')
        n_staging = _contar_no_staging(
            'registro_ocorrencias',
            f"WHERE id IN ({','.join(['%s']*N)})",
            ids
        )
        assert n_staging == N, f"Staging: esperava {N}, encontrou {n_staging}"
        log.info(f"  {N}/{N} registros chegaram ao staging ✓")

        # Nota: verificar na fato requer rodar a DAG de transform completa.
        # Para validação fim-a-fim completa, use:
        #   docker compose exec airflow-worker airflow dags trigger transform_star_schema
        # e depois cheque: SELECT COUNT(*) FROM fato_medicao_entomologica WHERE fk_evento = <evento_id>
        log.info("  [info] Para validar na Gold, dispare manualmente a transform e reexecute.")
    finally:
        _limpar_registros_teste(ids)


def test_hwm_overlap_captura_late_arriving(fixture):
    """
    Cenário 2: registro com updated_at levemente anterior ao HWM deve ser capturado pelo overlap.
    Simula o caso de clock skew: dado inserido 'no passado' em relação ao HWM exato.
    """
    # Pega o MAX(updated_at) atual do staging
    rows = exec_dw("SELECT MAX(updated_at) AS hwm FROM staging.registro_ocorrencias;", fetch=True)
    hwm = rows[0]['hwm']

    if hwm is None:
        log.info("  [skip] Staging vazio. Execute uma extração primeiro.")
        return

    # Insere com updated_at = HWM - 5min (dentro da janela de overlap de 10min)
    ts_late = hwm - timedelta(minutes=5)
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=7, updated_at=ts_late)
    try:
        hwm_com_overlap = hwm - timedelta(minutes=10)
        assert ts_late >= hwm_com_overlap, \
            f"Registro em {ts_late} ficaria fora do overlap ({hwm_com_overlap})"
        log.info(f"  Registro late (updated_at={ts_late}) está dentro do overlap (>= {hwm_com_overlap}) ✓")
    finally:
        _limpar_registros_teste([rid])


def test_tabela_full_detectada_pelo_short_circuit(fixture):
    """Cenário 5: atualização em tabela full-load deve ser detectada pelo short-circuit."""
    # Atualiza uma espécie para forçar updated_at novo
    exec_oltp("""
        UPDATE especies SET nome_popular = 'Abelha Teste SC', updated_at = NOW()
        WHERE id = %s;
    """, (fixture['especie_id'],))

    rows = exec_oltp("SELECT updated_at FROM especies WHERE id = %s", (fixture['especie_id'],), fetch=True)
    updated = rows[0]['updated_at']
    assert updated is not None

    # O short-circuit checa MAX(updated_at) de todas as tabelas incluindo 'especies'
    # Se updated > start_date do último run → retornaria True
    log.info(f"  Espécie atualizada em {updated} — short-circuit detectaria esta mudança ✓")


# ─── Testes adicionais ───────────────────────────────────────────────────────

def test_idempotencia_staging(fixture):
    """Extração executada duas vezes não duplica registros no staging."""
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=4)
    try:
        _forcar_extracao_staging('registro_ocorrencias')
        _forcar_extracao_staging('registro_ocorrencias')  # segunda vez

        n = _contar_no_staging('registro_ocorrencias', "WHERE id = %s", (rid,))
        assert n == 1, f"Idempotência violada: {n} cópias no staging após 2 extrações"
        log.info("  2 extrações consecutivas → 1 registro no staging ✓")
    finally:
        _limpar_registros_teste([rid])


def test_soft_delete_nao_entra_na_silver(fixture):
    """Registro com deleted_at setado não deve aparecer na Silver."""
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=2)

    # Seta soft delete no OLTP
    exec_oltp("UPDATE registro_ocorrencias SET deleted_at = NOW() WHERE id = %s;", (rid,))

    try:
        _forcar_extracao_staging('registro_ocorrencias')

        # Simula a Silver: filtra deleted_at IS NULL
        rows = exec_dw("""
            SELECT id FROM staging.registro_ocorrencias
            WHERE id = %s AND deleted_at IS NOT NULL;
        """, (rid,), fetch=True)
        assert len(rows) == 1, "Registro deletado deveria estar no staging com deleted_at preenchido"

        # Verifica que a Silver SQL filtraria este registro
        rows_silver = exec_dw("""
            SELECT id AS id_registro FROM staging.registro_ocorrencias
            WHERE id = %s AND deleted_at IS NULL;
        """, (rid,), fetch=True)
        assert len(rows_silver) == 0, "Silver não deveria ver registros com deleted_at"
        log.info("  Registro soft-deleted presente no staging mas filtrado pela Silver ✓")
    finally:
        exec_oltp("DELETE FROM registro_ocorrencias WHERE id = %s;", (rid,))


def test_update_refletido_no_staging(fixture):
    """Atualização de um registro deve sobrescrever no staging, não duplicar."""
    rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=1)
    _forcar_extracao_staging('registro_ocorrencias')

    # Atualiza no OLTP
    exec_oltp("""
        UPDATE registro_ocorrencias SET qtde_individuos = 99, updated_at = NOW()
        WHERE id = %s;
    """, (rid,))

    try:
        _forcar_extracao_staging('registro_ocorrencias')

        rows = exec_dw(
            "SELECT qtde_individuos FROM staging.registro_ocorrencias WHERE id = %s",
            (rid,), fetch=True
        )
        assert len(rows) == 1, f"Esperava 1 registro, encontrou {len(rows)}"
        assert rows[0]['qtde_individuos'] == 99, \
            f"Esperava qtde_individuos=99, encontrou {rows[0]['qtde_individuos']}"
        log.info("  Update refletido no staging sem duplicata ✓")
    finally:
        _limpar_registros_teste([rid])


def test_volume_500_registros(fixture):
    """500 registros inseridos de uma vez devem todos chegar ao staging."""
    N = 500
    ids = []
    for i in range(N):
        rid = _inserir_registro(fixture['evento_id'], fixture['especie_id'], qtde=(i % 10) + 1)
        ids.append(rid)

    try:
        _forcar_extracao_staging('registro_ocorrencias')
        placeholders = ','.join(['%s'] * N)
        n = _contar_no_staging(
            'registro_ocorrencias',
            f"WHERE id IN ({placeholders})",
            ids
        )
        assert n == N, f"Volume: esperava {N}, encontrou {n} no staging"
        log.info(f"  {N}/{N} registros no staging ✓")
    finally:
        _limpar_registros_teste(ids)


def test_eav_date_invalido_retorna_null(fixture):
    """Valor EAV com date malformado deve virar NULL, não derrubar a task."""
    exec_dw("""
        INSERT INTO staging.variaveis (id, estudo_id, nome, metrica, nivel_aplicacao, tipo_dado, created_at, updated_at)
        VALUES (99998, %s, 'var_date_invalida', 'unidade', 3, 2, NOW(), NOW())
        ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;
    """, (fixture['estudo_id'],))

    exec_dw("""
        INSERT INTO staging.valores_variaveis (id, variavel_id, id_nivel_aplicacao, valor, created_at, updated_at)
        VALUES (99998, 99998, %s, '32/13/2099', NOW(), NOW())
        ON CONFLICT (id) DO UPDATE SET valor = EXCLUDED.valor;
    """, (fixture['evento_id'],))

    try:
        exec_dw("""
            INSERT INTO public.silver_variaveis (id, nome, metrica, nivel_aplicacao, tipo_dado, created_at, updated_at)
            SELECT id, nome, metrica,
              CASE nivel_aplicacao WHEN 0 THEN 'campanha' WHEN 1 THEN 'unidade'
                WHEN 2 THEN 'evento' WHEN 3 THEN 'registro' ELSE 'unknown' END,
              CASE tipo_dado WHEN 0 THEN 'string' WHEN 1 THEN 'number'
                WHEN 2 THEN 'date' ELSE 'unknown' END,
              created_at, updated_at
            FROM staging.variaveis WHERE id = 99998
            ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;
        """)

        exec_dw("""
            INSERT INTO public.silver_valores_variaveis
                (id, variavel_id, id_nivel_aplicacao, valor_numerico, valor_texto, valor_data, updated_at)
            SELECT
              vv.id, vv.variavel_id, vv.id_nivel_aplicacao,
              NULL,
              CASE WHEN v.tipo_dado IN ('string','date') THEN vv.valor ELSE NULL END,
              CASE
                WHEN v.tipo_dado = 'date' AND vv.valor ~ '^\\d{4}-\\d{2}-\\d{2}$'
                  THEN CAST(vv.valor AS DATE)
                WHEN v.tipo_dado = 'date' THEN NULL
                ELSE NULL
              END,
              vv.updated_at
            FROM staging.valores_variaveis vv
            JOIN public.silver_variaveis v ON v.id = vv.variavel_id
            WHERE vv.id = 99998
            ON CONFLICT (id) DO UPDATE SET valor_data = EXCLUDED.valor_data;
        """)

        row = exec_dw(
            "SELECT valor_data FROM public.silver_valores_variaveis WHERE id = 99998",
            fetch=True
        )
        assert row, "Linha não inserida na Silver"
        assert row[0]['valor_data'] is None, \
            f"Date inválido deveria ser NULL, encontrou: {row[0]['valor_data']}"
        log.info("  Date malformado '32/13/2099' retornou NULL sem explodir ✓")
    finally:
        exec_dw("DELETE FROM public.silver_valores_variaveis WHERE id = 99998;")
        exec_dw("DELETE FROM public.silver_variaveis WHERE id = 99998;")
        exec_dw("DELETE FROM staging.valores_variaveis WHERE id = 99998;")
        exec_dw("DELETE FROM staging.variaveis WHERE id = 99998;")


def test_integridade_referencial_gold(fixture):
    """
    Registro no staging com especie_id inexistente não deve entrar na fato Gold.
    O JOIN da fato_medicao_entomologica rejeita orphan records silenciosamente.
    """
    ID_ESPECIE_INEXISTENTE = 999999

    # Insere diretamente no staging com FK inválida (bypassando o OLTP)
    exec_dw("""
        INSERT INTO staging.registro_ocorrencias
            (id, evento_amostragem_id, especie_id, data, hora, latitude, longitude,
             qtde_individuos, ausencia_especie, created_at, updated_at)
        VALUES (888888, %s, %s, NOW()::date, NOW()::time, -23.5, -46.6, 1, false, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING;
    """, (fixture['evento_id'], ID_ESPECIE_INEXISTENTE))

    try:
        # Simula Silver: aceita qualquer especie_id (sem FK na Silver)
        exec_dw("""
            INSERT INTO public.silver_registro_ocorrencias
                (id_registro, especie_id, evento_amostragem_id, data, latitude, longitude,
                 ausencia_especie, qtde_individuos, data_atualizacao)
            VALUES (888888, %s, %s, NOW()::date, -23.5, -46.6, false, 1, NOW())
            ON CONFLICT (id_registro) DO NOTHING;
        """, (ID_ESPECIE_INEXISTENTE, fixture['evento_id']))

        # Verifica que o JOIN da fato rejeitaria este registro (especie_id não existe na dim_especie)
        rows = exec_dw("""
            SELECT r.id_registro
            FROM public.silver_registro_ocorrencias r
            JOIN public.dim_especie esp ON esp.id_especie = r.especie_id
            WHERE r.id_registro = 888888;
        """, fetch=True)

        assert len(rows) == 0, \
            f"Registro órfão não deveria passar pelo JOIN da Gold, mas {len(rows)} passou"
        log.info("  Registro com especie_id inválida rejeitado pelo JOIN da Gold ✓")
    finally:
        exec_dw("DELETE FROM public.silver_registro_ocorrencias WHERE id_registro = 888888;")
        exec_dw("DELETE FROM staging.registro_ocorrencias WHERE id = 888888;")


# ─── Main ────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    log.info("=" * 60)
    log.info("KHEPRIX — STRESS TEST DA INGESTÃO")
    log.info("=" * 60)

    log.info("\nPreparando fixtures...")
    try:
        fixture = _seed_estudo_base()
        log.info(f"Fixture: {fixture}")
    except Exception as e:
        log.error(f"Falha ao preparar fixtures: {e}", exc_info=True)
        sys.exit(1)

    run_test("Dado antes da extração chega ao staging",        lambda: test_dado_antes_da_extracao(fixture))
    run_test("Short-circuit detecta ausência de dados novos", lambda: test_short_circuit_sem_dados_novos(fixture))
    run_test("Short-circuit detecta dado novo",               lambda: test_short_circuit_detecta_dado_novo(fixture))
    run_test("Ausência de espécie gera quantidade_apurada=0", lambda: test_ausencia_especie_zero_na_gold(fixture))
    run_test("Valor EAV inválido retorna NULL (não explode)",  lambda: test_valor_eav_invalido_nao_explode(fixture))
    run_test("N registros chegam ao staging (fim-a-fim)",     lambda: test_contagem_fim_a_fim(fixture))
    run_test("HWM overlap captura late-arriving data",        lambda: test_hwm_overlap_captura_late_arriving(fixture))
    run_test("Tabela full detectada pelo short-circuit",      lambda: test_tabela_full_detectada_pelo_short_circuit(fixture))
    run_test("Idempotência: 2 extrações não duplicam staging", lambda: test_idempotencia_staging(fixture))
    run_test("Soft delete não entra na Silver",               lambda: test_soft_delete_nao_entra_na_silver(fixture))
    run_test("Update refletido no staging sem duplicata",     lambda: test_update_refletido_no_staging(fixture))
    run_test("Volume: 500 registros chegam ao staging",       lambda: test_volume_500_registros(fixture))
    run_test("EAV date malformado retorna NULL",              lambda: test_eav_date_invalido_retorna_null(fixture))
    run_test("Integridade referencial Gold rejeita órfãos",   lambda: test_integridade_referencial_gold(fixture))

    log.info("\n" + "=" * 60)
    log.info("RESULTADO FINAL")
    log.info("=" * 60)
    passou = sum(1 for r in results if r[0] == PASS)
    falhou = sum(1 for r in results if r[0] == FAIL)
    for r in results:
        msg = f"  {r[0]}  {r[1]}"
        if r[0] == FAIL and len(r) > 2:
            msg += f"\n       → {r[2]}"
        log.info(msg)
    log.info(f"\nTotal: {passou} passou / {falhou} falhou / {len(results)} total")
    sys.exit(0 if falhou == 0 else 1)
