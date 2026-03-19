from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.dummy import DummyOperator
from airflow.operators.python import PythonOperator

# Argumentos padrão
default_args = {
    'owner': 'admin',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

# Definição da DAG
with DAG(
    'etl_kheprix_pipeline',
    default_args=default_args,
    description='Pipeline de ETL para mover dados do Kheprix OLTP para o Data Warehouse',
    schedule_interval='@daily',
    start_date=datetime(2025, 1, 1),
    catchup=False,
    tags=['kheprix', 'etl'],
) as dag:

    # 1. Sincronização: Busca os dados validados do banco OLTP
    def extract_from_oltp(**kwargs):
        print("Extraindo dados sincronizados do OLTP...")
        # Lógica de extração com PostgresHook aqui
        pass

    task_extract = PythonOperator(
        task_id='extract_from_oltp',
        python_callable=extract_from_oltp,
    )

    # 2. Data Cleansing: Limpeza e normalização
    def data_cleansing(**kwargs):
        print("Limpando e normalizando dados...")
        pass

    task_cleansing = PythonOperator(
        task_id='data_cleansing',
        python_callable=data_cleansing,
    )

    # 3. Cálculo de Índices: Shannon-Wiener, Simpson, Equitabilidade
    def calculate_indexes(**kwargs):
        print("Calculando índices de biodiversidade...")
        pass

    task_calculate = PythonOperator(
        task_id='calculate_indexes',
        python_callable=calculate_indexes,
    )

    # 4. Carga: Insere no modelo dimensional (Star Schema)
    def load_to_dw(**kwargs):
        print("Carregando dados no Data Warehouse...")
        pass

    task_load = PythonOperator(
        task_id='load_to_dw',
        python_callable=load_to_dw,
    )

    # Definindo a ordem das tarefas
    task_extract >> task_cleansing >> task_calculate >> task_load
