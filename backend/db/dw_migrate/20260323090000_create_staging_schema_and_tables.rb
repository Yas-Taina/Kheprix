class CreateStagingSchemaAndTables < ActiveRecord::Migration[8.0]
  ## Tabelas espelho da aplicação, vão ser usadas para não sobrecarregar o banco transacional
  def up
    execute "CREATE SCHEMA IF NOT EXISTS staging;"

    execute <<-SQL
      CREATE TABLE IF NOT EXISTS staging.usuarios (id INT PRIMARY KEY, nome VARCHAR, email VARCHAR, password_digest VARCHAR, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.estudos (id INT PRIMARY KEY, nome VARCHAR, observacoes TEXT, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.colaboradores (estudo_id INT, usuario_id INT, perfil INT, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.campanhas (id INT PRIMARY KEY, estudo_id INT, nome VARCHAR, data_inicio DATE, data_fim DATE, descricao TEXT, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.especies (id INT PRIMARY KEY, estudo_id INT, foto VARCHAR, classe VARCHAR, genero VARCHAR, nome_popular VARCHAR, nome_cientifico VARCHAR, status_conservacao VARCHAR, nativa_da_regiao BOOLEAN, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.unidades_amostrais (id INT PRIMARY KEY, campanha_id INT, latitude DECIMAL, longitude DECIMAL, raio DECIMAL, metodo_coleta TEXT, esforco_amostral TEXT, created_at TIMESTAMP, updated_at TIMESTAMP, nome VARCHAR, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.eventos_amostragem (id INT PRIMARY KEY, unidade_amostral_id INT, horario_inicio TIMESTAMP, horario_fim TIMESTAMP, esforco_real TEXT, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.variaveis (id INT PRIMARY KEY, estudo_id INT, nome VARCHAR, metrica VARCHAR, nivel_aplicacao INT, tipo_dado INT, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
      CREATE TABLE IF NOT EXISTS staging.valores_variaveis (id INT PRIMARY KEY, variavel_id INT, id_nivel_aplicacao INT, valor TEXT, created_at TIMESTAMP, updated_at TIMESTAMP, loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
    SQL
  end

  def down
    execute "DROP SCHEMA IF EXISTS staging CASCADE;"
  end
end
