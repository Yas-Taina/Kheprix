# frozen_string_literal: true

# Trava de Segurança
current_db = ActiveRecord::Base.connection_db_config.name.to_s
if current_db.include?("dw")
  puts "🚫 Erro: Seed não pode rodar no DW."
  exit
end

puts "🌱 Iniciando Seed Hierárquico Completo (Simulação de Dados Ecológicos realistas)..."

# 1. Limpeza
puts "Limpando registros antigos..."
ValorVariavel.delete_all
Variavel.delete_all
RegistroOcorrencia.delete_all
EventoAmostragem.delete_all
UnidadeAmostral.delete_all
Especie.delete_all
Campanha.delete_all
Colaborador.delete_all
Estudo.delete_all
Usuario.delete_all

# --- DADOS DE APOIO ---
FASES_LUA = ["Nova", "Crescente", "Cheia", "Minguante"]
TIPOS_MATA = ["Primária", "Secundária (Capoeira)", "Mata de Galeria", "Várzea"]
METODOS_COLETA = ["Armadilha Malaise", "Pitfall", "Rede Entomológica", "Guarda-chuva Entomológico"]

# 2. Usuários
puts "Criando 20 Usuários Genéricos..."
NOMES_GENERICOS = [
  "Dr. Ricardo Silva", "Dra. Helena Souza", "Msc. Roberto Oliveira", 
  "Profa. Beatriz Costa", "Pesquisador João Santos", "Dra. Clarice Mendes",
  "Msc. André Lima", "Bio. Patrícia Rocha", "Dr. Fernando Almeida",
  "Pesquisadora Aline Ferreira", "Msc. Carlos Eduardo", "Dra. Sofia Martins",
  "Dr. Lucas Pereira", "Bio. Renata Gomes", "Msc. Juliana Lopes",
  "Dr. Marcos Vinícius", "Dra. Amanda Silva", "Pesquisador Igor Souza",
  "Msc. Camila Diniz", "Bio. Bruno Santos"
]

usuarios_base = []
NOMES_GENERICOS.each_with_index do |nome, i|
  usuarios_base << Usuario.create!(
    nome: nome, 
    email: "pesquisador#{i + 1}@kheprix.com", 
    password: "password123", 
    password_confirmation: "password123"
  )
end

# 3. Estudos (5 cenários diferentes)
puts "Criando Estudos e Variáveis..."
CENARIOS = [
  { nome: "Biodiversidade de Coleoptera no Cerrado", especie_base: "Besouro" },
  { nome: "Monitoramento de Culicidae em Áreas Urbanas", especie_base: "Mosquito" },
  { nome: "Fauna de Formicidae em Gradiente Altitudinal", especie_base: "Formiga" },
  { nome: "Comunidade de Lepidoptera em Área de Reflorestamento", especie_base: "Borboleta" },
  { nome: "Abundância de Diptera em Mata Atlântica", especie_base: "Mosca" }
]

EPITETOS = ["exemplaris", "secundus", "tertius"]
ESPECIES_CONFIG = [
  { status: 'LC', endemismo: false },
  { status: 'VU', endemismo: true  },
  { status: 'CR', endemismo: true  },
]

CENARIOS.each do |cenario|
  estudo = Estudo.create!(nome: cenario[:nome], observacoes: "Estudo acadêmico para TCC.")

  # Atribui Colaboradores Aleatórios
  equipe = usuarios_base.sample(3)
  Colaborador.create!(estudo: estudo, usuario: equipe[0], perfil: :proprietario)
  Colaborador.create!(estudo: estudo, usuario: equipe[1], perfil: :colaborador)
  Colaborador.create!(estudo: estudo, usuario: equipe[2], perfil: :colaborador)

  # --- DEFINIÇÃO DE VARIÁVEIS POR NÍVEL ---
  var_campanha = Variavel.create!(estudo: estudo, nome: "Fase da Lua", metrica: "Nome", nivel_aplicacao: :campanha, tipo_dado: :string)
  var_unidade = Variavel.create!(estudo: estudo, nome: "Tipo de Mata", metrica: "Nome", nivel_aplicacao: :unidade, tipo_dado: :string)
  var_evento = Variavel.create!(estudo: estudo, nome: "Temperatura do Ar", metrica: "Celsius", nivel_aplicacao: :evento, tipo_dado: :number)
  var_registro = Variavel.create!(estudo: estudo, nome: "Massa do Lote", metrica: "Gramas", nivel_aplicacao: :registro, tipo_dado: :number)

  # Criar 3 espécies específicas para este estudo
  especies = []
  3.times do |i|
    especies << Especie.create!(
      estudo: estudo,
      nome_popular: "#{cenario[:especie_base]} #{i+1}",
      genero: "#{cenario[:especie_base].first}#{i}",
      especie: EPITETOS[i],
      classe: "Insecta",
      status_conservacao: ESPECIES_CONFIG[i][:status],
      endemismo: ESPECIES_CONFIG[i][:endemismo]
    )
  end

  # --- HIERARQUIA DE COLETA ---
  # 1 Campanha
  campanha = Campanha.create!(
    estudo: estudo, 
    nome: "Campanha Principal - #{estudo.nome}", 
    data_inicio: 6.months.ago, 
    data_fim: Date.today
  )
  ValorVariavel.create!(variavel: var_campanha, id_nivel_aplicacao: campanha.id, valor: FASES_LUA.sample)

  # 2 Unidades Amostrais
  2.times do |u_idx|
    unidade = UnidadeAmostral.create!(
      campanha: campanha,
      nome: "Ponto #{u_idx + 1}",
      latitude: -23.5489 + (rand * 0.1),
      longitude: -46.6388 + (rand * 0.1),
      raio: 50.0,
      metodo_coleta: METODOS_COLETA.sample
    )
    ValorVariavel.create!(variavel: var_unidade, id_nivel_aplicacao: unidade.id, valor: TIPOS_MATA.sample)

    # 3 Eventos de Amostragem por Unidade
    3.times do |e_idx|
      evento = EventoAmostragem.create!(
        unidade_amostral: unidade,
        horario_inicio: (e_idx + 1).days.ago.change(hour: 8),
        horario_fim: (e_idx + 1).days.ago.change(hour: 12),
        esforco_real: "4 horas de coleta ativa"
      )
      ValorVariavel.create!(variavel: var_evento, id_nivel_aplicacao: evento.id, valor: rand(22.0..32.0).round(1).to_s)

      # 5 Registros de Ocorrência por Evento
      5.times do
        especie = especies.sample
        registro = RegistroOcorrencia.create!(
          evento_amostragem: evento,
          especie: especie,
          data: evento.horario_inicio.to_date,
          hora: evento.horario_inicio.strftime("%H:%M"),
          latitude: unidade.latitude + (rand * 0.001),
          longitude: unidade.longitude + (rand * 0.001),
          qtde_individuos: rand(1..20),
          ausencia_especie: false
        )
        # Valor de variável para o registro (Nível 3)
        ValorVariavel.create!(variavel: var_registro, id_nivel_aplicacao: registro.id, valor: (rand * 50).round(2).to_s)
      end
    end
  end
end

puts "✅ Seed concluído!"
puts "Estatísticas:"
puts "- Usuários: #{Usuario.count}"
puts "- Estudos: #{Estudo.count}"
puts "- Registros de Ocorrência: #{RegistroOcorrencia.count}"
puts "- Valores de Variáveis: #{ValorVariavel.count}"
