# frozen_string_literal: true

# Cria um usuário PostgreSQL dedicado ao chatbot com acesso somente-leitura.
#
# Princípio do menor privilégio: o chatbot só precisa de SELECT nas tabelas
# de consumo do DW. Este usuário não pode INSERT, UPDATE, DELETE ou acessar
# tabelas de staging/silver, mesmo que um SQL inválido escape dos guard rails.
#
# O connection pool do chatbot (db.py) configura a sessão como read-only via
# set_session(readonly=True) — esta migração adiciona uma segunda camada:
# o próprio usuário de banco não tem permissão de escrita.
#
# Variável de ambiente lida em tempo de migração (disponível via docker-compose):
#   POSTGRES_DW_CHATBOT_PASSWORD — senha do usuário kheprix_chatbot_ro

class CreateChatbotReadonlyUser < ActiveRecord::Migration[8.0]
  CHATBOT_ROLE = "kheprix_chatbot_ro"

  def up
    password = ENV.fetch("POSTGRES_DW_CHATBOT_PASSWORD", "chatbot_senha_local")
    quoted_password = ActiveRecord::Base.connection.quote(password)

    execute <<~SQL
      DO $$
      BEGIN
        IF NOT EXISTS (
          SELECT FROM pg_catalog.pg_roles WHERE rolname = '#{CHATBOT_ROLE}'
        ) THEN
          CREATE ROLE #{CHATBOT_ROLE}
            WITH LOGIN
            PASSWORD #{quoted_password}
            NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
        ELSE
          ALTER ROLE #{CHATBOT_ROLE} PASSWORD #{quoted_password};
        END IF;
      END
      $$;

      GRANT CONNECT ON DATABASE "#{connection.current_database}" TO #{CHATBOT_ROLE};
      GRANT USAGE ON SCHEMA public TO #{CHATBOT_ROLE};

      -- Apenas SELECT nas tabelas de consumo (presentation layer)
      GRANT SELECT ON public.indicadores_dashboard  TO #{CHATBOT_ROLE};
      GRANT SELECT ON public.analises_estatisticas  TO #{CHATBOT_ROLE};

      -- Garante que futuras tabelas no schema public NÃO sejam acessíveis por padrão
      ALTER DEFAULT PRIVILEGES IN SCHEMA public
        REVOKE ALL ON TABLES FROM #{CHATBOT_ROLE};
    SQL
  end

  def down
    execute <<~SQL
      REVOKE SELECT ON public.indicadores_dashboard  FROM #{CHATBOT_ROLE};
      REVOKE SELECT ON public.analises_estatisticas  FROM #{CHATBOT_ROLE};
      REVOKE USAGE   ON SCHEMA public               FROM #{CHATBOT_ROLE};
      REVOKE CONNECT ON DATABASE "#{connection.current_database}" FROM #{CHATBOT_ROLE};
      DROP ROLE IF EXISTS #{CHATBOT_ROLE};
    SQL
  end
end
