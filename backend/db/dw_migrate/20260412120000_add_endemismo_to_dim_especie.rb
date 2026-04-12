class AddEndemismoToDimEspecie < ActiveRecord::Migration[8.0]
  def change
    add_column :dim_especie, :endemismo, :boolean, default: false
  end
end
