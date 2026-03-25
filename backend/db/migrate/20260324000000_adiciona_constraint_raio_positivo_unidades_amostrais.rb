# frozen_string_literal: true

class AdicionaConstraintRaioPositivoUnidadesAmostrais < ActiveRecord::Migration[8.0]
  def up
    execute <<~SQL
      ALTER TABLE unidades_amostrais
        ADD CONSTRAINT raio_deve_ser_positivo CHECK (raio IS NULL OR raio > 0);
    SQL
  end

  def down
    execute <<~SQL
      ALTER TABLE unidades_amostrais
        DROP CONSTRAINT raio_deve_ser_positivo;
    SQL
  end
end
