# frozen_string_literal: true

namespace :db do
  namespace :migrate do
    desc "Executa as migrações apenas do Data Warehouse (DW)"
    task dw: :environment do
      puts "🚀 Iniciando migrações do Data Warehouse (DW)..."

      ActiveRecord::Base.establish_connection :dw
      connection = ActiveRecord::Base.connection

      schema_migration = connection.pool.schema_migration
      context = ActiveRecord::MigrationContext.new("db/dw_migrate", schema_migration)
      context.migrate

      puts "✅ Migrações do DW concluídas!"
    end

    desc "Reseta o banco do Data Warehouse (DW) - USE COM CAUTELA"
    task dw_reset: :environment do
      puts "⚠️  Limpando banco do Data Warehouse (DW)..."

      ActiveRecord::Base.establish_connection :dw
      connection = ActiveRecord::Base.connection

      connection.execute("DROP SCHEMA IF EXISTS staging CASCADE;")
      connection.execute("DROP SCHEMA IF EXISTS public CASCADE;")
      connection.execute("CREATE SCHEMA public;")
      connection.execute("DROP TABLE IF EXISTS schema_migrations CASCADE;")
      connection.execute("DROP TABLE IF EXISTS ar_internal_metadata CASCADE;")

      puts "♻️  Rodando migrações do zero..."

      Rake::Task["db:migrate:dw"].reenable
      Rake::Task["db:migrate:dw"].invoke
    end
  end
end
