from datetime import datetime
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.utils.task_group import TaskGroup
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))
from utils.db_helpers import execute_upsert
from utils.etl_log import registrar_inicio, registrar_conclusao, registrar_falha

default_args = {
    'owner': 'admin',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0,
}

TASKS_CONFIG = {
    'silver_especies':             {'target': 'public.silver_especies',             'pks': ['id_especie'],                            'sql': 'silver/silver_especies.sql',             'conflict': 'UPDATE'},
    'silver_variaveis':            {'target': 'public.silver_variaveis',            'pks': ['id'],                                    'sql': 'silver/silver_variaveis.sql',            'conflict': 'UPDATE'},
    'silver_estudos':              {'target': 'public.silver_estudos',              'pks': ['id_estudo'],                             'sql': 'silver/silver_estudos.sql',              'conflict': 'UPDATE'},
    'silver_campanhas':            {'target': 'public.silver_campanhas',            'pks': ['id_campanha'],                           'sql': 'silver/silver_campanhas.sql',            'conflict': 'UPDATE'},
    'silver_unidades_amostrais':   {'target': 'public.silver_unidades_amostrais',   'pks': ['id_unidade'],                            'sql': 'silver/silver_unidades_amostrais.sql',   'conflict': 'UPDATE'},
    'silver_eventos_amostragem':   {'target': 'public.silver_eventos_amostragem',   'pks': ['id_evento'],                             'sql': 'silver/silver_eventos_amostragem.sql',   'conflict': 'UPDATE'},
    'silver_registro_ocorrencias': {'target': 'public.silver_registro_ocorrencias', 'pks': ['id_registro'],                           'sql': 'silver/silver_registro_ocorrencias.sql', 'conflict': 'UPDATE'},
    'silver_valores_variaveis':    {'target': 'public.silver_valores_variaveis',    'pks': ['id'],                                    'sql': 'silver/silver_valores_variaveis.sql',    'conflict': 'UPDATE'},
    'silver_variaveis_agregadas':  {'target': 'public.silver_variaveis_agregadas',  'pks': ['id_nivel_aplicacao', 'nivel_aplicacao'], 'sql': 'silver/silver_variaveis_agregadas.sql',  'conflict': 'UPDATE'},
    'gold_dim_tempo':               {'target': 'public.dim_tempo',               'pks': ['id_data'],      'sql': 'gold/dimensions/dim_tempo.sql',               'conflict': 'NOTHING'},
    'gold_dim_especie':             {'target': 'public.dim_especie',             'pks': ['id_especie'],   'sql': 'gold/dimensions/dim_especie.sql',             'conflict': 'UPDATE'},
    'gold_dim_variavel':            {'target': 'public.dim_variavel',            'pks': ['id'],           'sql': 'gold/dimensions/dim_variavel.sql',            'conflict': 'UPDATE'},
    'gold_dim_estudo':              {'target': 'public.dim_estudo',              'pks': ['id_estudo'],    'sql': 'gold/dimensions/dim_estudo.sql',              'conflict': 'UPDATE'},
    'gold_dim_campanha':            {'target': 'public.dim_campanha',            'pks': ['id_campanha'],  'sql': 'gold/dimensions/dim_campanha.sql',            'conflict': 'UPDATE'},
    'gold_dim_unidade_amostral':    {'target': 'public.dim_unidade_amostral',    'pks': ['id_unidade'],   'sql': 'gold/dimensions/dim_unidade_amostral.sql',    'conflict': 'UPDATE'},
    'gold_dim_evento_amostragem':   {'target': 'public.dim_evento_amostragem',   'pks': ['id_evento'],    'sql': 'gold/dimensions/dim_evento_amostragem.sql',   'conflict': 'UPDATE'},
    'gold_dim_registro_ocorrencia': {'target': 'public.dim_registro_ocorrencia', 'pks': ['id_registro'],  'sql': 'gold/dimensions/dim_registro_ocorrencia.sql', 'conflict': 'UPDATE'},
    'gold_fato_medicao_entomologica': {'target': 'public.fato_medicao_entomologica', 'pks': ['id_registro'],                'sql': 'gold/facts/fato_medicao_entomologica.sql', 'conflict': 'UPDATE'},
    'gold_fato_variaveis_unificadas': {'target': 'public.fato_variaveis_unificadas', 'pks': ['id_registro', 'id_variavel'], 'sql': 'gold/facts/fato_variaveis_unificadas.sql', 'conflict': 'UPDATE'},
    'gold_presentation_indicadores': {'target': 'public.indicadores_dashboard', 'pks': ['id_registro'],                'sql': 'gold/presentation/indicadores_dashboard.sql', 'conflict': 'UPDATE'},
    'gold_presentation_analises':    {'target': 'public.analises_estatisticas', 'pks': ['id_registro', 'id_variavel'], 'sql': 'gold/presentation/analises_estatisticas.sql',  'conflict': 'UPDATE'},
}

DAG_ID = 'transform_star_schema'

XCOM_TASK_IDS = (
    [f'silver.{k}'         for k in TASKS_CONFIG if k.startswith('silver_')] +
    [f'gold.dimensoes.{k}' for k in TASKS_CONFIG if 'dim'  in k] +
    [f'gold.fatos.{k}'     for k in TASKS_CONFIG if 'fato' in k] +
    [f'gold.presentation.{k}'     for k in TASKS_CONFIG if k.startswith('gold_presentation_')]
)


def _upsert_kwargs(config: dict) -> dict:
    return {
        'target_table':    config['target'],
        'primary_keys':    config['pks'],
        'sql_file':        config['sql'],
        'conflict_action': config['conflict'],
    }


with DAG(
    DAG_ID,
    default_args=default_args,
    description='Pipeline ETL Medallion: Silver → Gold (Star Schema Kimball)',
    schedule=None,
    start_date=datetime(2025, 1, 1),
    catchup=False,
    on_failure_callback=lambda ctx: registrar_falha(DAG_ID, ctx),
    tags=['kheprix', 'etl', 'medallion', 'kimball']
) as dag:

    task_log_inicio = PythonOperator(
        task_id='log_inicio',
        python_callable=lambda **kw: registrar_inicio(DAG_ID, kw['run_id']),
    )

    tasks = {}

    with TaskGroup('silver') as tg_silver:
        for t_id, config in {k: v for k, v in TASKS_CONFIG.items() if k.startswith('silver_')}.items():
            tasks[t_id] = PythonOperator(
                task_id=t_id,
                python_callable=execute_upsert,
                op_kwargs=_upsert_kwargs(config),
            )

    with TaskGroup('gold') as tg_gold:
        with TaskGroup('dimensoes') as tg_dims:
            for t_id, config in {k: v for k, v in TASKS_CONFIG.items() if 'dim' in k}.items():
                tasks[t_id] = PythonOperator(
                    task_id=t_id,
                    python_callable=execute_upsert,
                    op_kwargs=_upsert_kwargs(config),
                )

        with TaskGroup('fatos') as tg_fatos:
            for t_id, config in {k: v for k, v in TASKS_CONFIG.items() if 'fato' in k}.items():
                tasks[t_id] = PythonOperator(
                    task_id=t_id,
                    python_callable=execute_upsert,
                    op_kwargs=_upsert_kwargs(config),
                )

        with TaskGroup('presentation') as tg_marts:
            for t_id, config in {k: v for k, v in TASKS_CONFIG.items() if k.startswith('gold_presentation_')}.items():
                tasks[t_id] = PythonOperator(
                    task_id=t_id,
                    python_callable=execute_upsert,
                    op_kwargs=_upsert_kwargs(config),
                )

    task_log_conclusao = PythonOperator(
        task_id='log_conclusao',
        python_callable=lambda **kw: registrar_conclusao(DAG_ID, kw['run_id'], XCOM_TASK_IDS, kw['ti']),
    )

    task_log_inicio >> tg_silver

    tasks['silver_variaveis'] >> tasks['silver_valores_variaveis'] >> tasks['silver_variaveis_agregadas']

    tasks['silver_especies']             >> tasks['gold_dim_especie']
    tasks['silver_variaveis']            >> tasks['gold_dim_variavel']
    tasks['silver_registro_ocorrencias'] >> tasks['gold_dim_tempo']
    tasks['silver_estudos']              >> tasks['gold_dim_estudo']

    [tasks['silver_campanhas'],            tasks['gold_dim_estudo'],          tasks['silver_variaveis_agregadas']] >> tasks['gold_dim_campanha']
    [tasks['silver_unidades_amostrais'],   tasks['gold_dim_campanha']]                                            >> tasks['gold_dim_unidade_amostral']
    [tasks['silver_eventos_amostragem'],   tasks['gold_dim_unidade_amostral']]                                    >> tasks['gold_dim_evento_amostragem']
    [tasks['silver_registro_ocorrencias'], tasks['gold_dim_evento_amostragem']]                                   >> tasks['gold_dim_registro_ocorrencia']

    [tasks['gold_dim_tempo'], tasks['gold_dim_especie'], tasks['gold_dim_registro_ocorrencia']] >> tasks['gold_fato_medicao_entomologica']
    [tasks['gold_dim_registro_ocorrencia'], tasks['gold_dim_variavel']]                         >> tasks['gold_fato_variaveis_unificadas']

    [tasks['gold_fato_medicao_entomologica'], tasks['gold_fato_variaveis_unificadas']] >> tasks['gold_presentation_indicadores']
    [tasks['gold_fato_medicao_entomologica'], tasks['gold_fato_variaveis_unificadas']] >> tasks['gold_presentation_analises']

    [tasks['gold_presentation_indicadores'], tasks['gold_presentation_analises']] >> task_log_conclusao
