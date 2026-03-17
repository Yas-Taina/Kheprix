# frozen_string_literal: true

# Trava de Segurança: Garantir que o seed rode Apenas no OLTP (primary)
current_db = ActiveRecord::Base.connection_db_config.name.to_s
if current_db.include?("dw") || current_db.include?("datawarehouse")
  puts "🚫 Operação cancelada: Este seed foi projetado apenas para o banco Transacional (OLTP)."
  puts "Banco atual detectado: #{current_db}"
  exit
end

puts "🌱 Iniciando o povoamento do banco de dados OLTP (Transacional)..."

# Limpeza opcional (cuidado em PRD, mas útil em dev)
puts "Limpando registros antigos..."
ValorVariavel.delete_all
Variavel.delete_all
Especie.delete_all
Campanha.delete_all
Colaborador.delete_all
Estudo.delete_all
Usuario.delete_all

# 1. Criar 10 Usuários
puts "Criando 10 Usuários..."
usuarios = []
10.times do |i|
  usuarios << Usuario.create!(
    nome: "Pesquisador #{i + 1}",
    email: "pesquisador#{i + 1}@kheprix.com",
    password: "senha_segura_123", # Password_digest será gerado pelo has_secure_password se o modelo tiver
    password_confirmation: "senha_segura_123"
  )
end

# 2. Criar 5 Estudos Iniciais
puts "Criando 5 Estudos..."
estudos = []
5.times do |i|
  estudos << Estudo.create!(
    nome: "Monitoramento de Bioindicadores - Região #{('A'..'E').to_a[i]}",
    observacoes: "Estudo longitudinal para avaliar a riqueza e abundância de insetos na região #{('A'..'E').to_a[i]}."
  )
end

# Vincular colaboradores aos estudos
estudos.each do |estudo|
  # Atribui 2 pesquisadores aleatórios para cada estudo
  pesquisadores_estudo = usuarios.sample(2)
  pesquisadores_estudo.each do |usuario|
    Colaborador.create!(
      estudo_id: estudo.id,
      usuario_id: usuario.id,
      perfil: [:colaborador, :proprietario].sample
    )
  end
end

# 3. Criar Espécies Base e Campanhas
# Vamos criar algumas tabelas de contexto para receber os 1000 registros
especies_geradas = []
estudos.each do |estudo|
  3.times do |i|
    especies_geradas << Especie.create!(
      estudo_id: estudo.id,
      nome_popular: "Inseto Teste #{i}-#{estudo.id}",
      nome_cientifico: "Insecta brasiliensis var #{i}-#{estudo.id}",
      classe: "Insecta",
      nativa_da_regiao: true
    )
  end

  # Criar uma Campanha representativa para o estudo
  Campanha.create!(
    estudo_id: estudo.id,
    nome: "Campanha Anual #{estudo.nome}",
    data_inicio: 1.year.ago.to_date,
    data_fim: Date.today,
    descricao: "Coletas ao longo dos últimos 12 meses."
  )
end

# 4. Criar Variáveis (Medições)
# Nível 0 = campanha, 1 = unidade, 2 = evento, 3 = registro. 
# Usaremos :registro (3) e tipo :number (1) para simular abundância de espécimes coletados.
variaveis = []
estudos.each do |estudo|
  variaveis << Variavel.create!(
    estudo_id: estudo.id,
    nome: "Abundância da Espécie",
    metrica: "Indivíduos",
    nivel_aplicacao: 3, # registro
    tipo_dado: 1        # number
  )
end

# 5. Gerar 1000 registros de "Coletas" / Valores Variáveis
# Variando as datas ao longo dos últimos 12 meses (para testes temporais).
puts "Gerando 1000 medições/coletas ao longo dos últimos 12 meses..."
valores_inseridos = 0

# Vamos dividir os 1000 registros proporcionalmente entre as variáveis dos estudos
registros_por_variavel = 1000 / variaveis.size

variaveis.each do |variavel|
  estudo = variavel.estudo
  especies_do_estudo = especies_geradas.select { |e| e.estudo_id == estudo.id }
  
  registros_por_variavel.times do
    # Gera uma data aleatória nos últimos 365 dias
    random_days_ago = rand(0..365)
    random_date = random_days_ago.days.ago

    # Simularemos a vinculação ao ID da espécie (id_nivel_aplicacao atua como FK polimórfica conceitual neste schema)
    especie_alvo = especies_do_estudo.sample
    
    ValorVariavel.create!(
      variavel_id: variavel.id,
      id_nivel_aplicacao: especie_alvo.id,
      valor: rand(1..50).to_s, # Simulando de 1 a 50 indivíduos encontrados
      created_at: random_date,
      updated_at: random_date
    )
    valores_inseridos += 1
  end
end

puts "✅ Seed concluído com sucesso!"
puts "📊 Resumo da Base (OLTP):"
puts " - Usuários: #{Usuario.count}"
puts " - Estudos: #{Estudo.count}"
puts " - Espécies: #{Especie.count}"
puts " - Campanhas: #{Campanha.count}"
puts " - Variáveis: #{Variavel.count}"
puts " - Medições (ValorVariavel): #{ValorVariavel.count}"
