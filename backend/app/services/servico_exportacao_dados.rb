# frozen_string_literal: true

class ServicoExportacaoDados
  def gerar_xml(estudo:)
    require "builder"

    variaveis = carregar_variaveis(estudo)
    valores_map = carregar_valores(estudo)
    variaveis_por_nivel = variaveis.group_by(&:nivel_aplicacao)

    campanhas = estudo.campanhas
      .includes(
        unidades_amostrais: {
          eventos_amostragem: {
            registro_ocorrencias: :especie
          }
        }
      )
      .order("campanhas.nome")

    xml = Builder::XmlMarkup.new(indent: 2)
    xml.instruct! :xml, version: "1.0", encoding: "UTF-8"

    xml.estudo do
      xml.nome(estudo.nome)
      xml.observacoes(estudo.observacoes)

      xml.especies do
        estudo.especies.order(:nome_popular).each do |esp|
          xml.especie do
            xml.classe(esp.classe)
            xml.ordem(esp.ordem)
            xml.familia(esp.familia)
            xml.genero(esp.genero)
            xml.especie(esp.especie)
            xml.nome_popular(esp.nome_popular)
            xml.status_conservacao(esp.status_conservacao)
            xml.endemismo(esp.endemismo)
          end
        end
      end

      xml.variaveis do
        variaveis.each do |v|
          xml.variavel do
            xml.nome(v.nome)
            xml.nivel_aplicacao(v.nivel_aplicacao)
            xml.tipo_dado(v.tipo_dado)
            xml.metrica(v.metrica)
          end
        end
      end

      xml.campanhas do
        campanhas.each do |campanha|
          xml.campanha do
            xml.nome(campanha.nome)
            xml.data_inicio(campanha.data_inicio)
            xml.data_fim(campanha.data_fim)
            xml.descricao(campanha.descricao)
            escrever_valores_variaveis(xml, variaveis_por_nivel["campanha"], valores_map, campanha.id)

            xml.unidades_amostrais do
              campanha.unidades_amostrais.each do |unidade|
                xml.unidade_amostral do
                  xml.nome(unidade.nome)
                  xml.latitude(unidade.latitude)
                  xml.longitude(unidade.longitude)
                  xml.raio(unidade.raio)
                  xml.metodo_coleta(unidade.metodo_coleta)
                  xml.esforco_amostral(unidade.esforco_amostral)
                  escrever_valores_variaveis(xml, variaveis_por_nivel["unidade"], valores_map, unidade.id)

                  xml.eventos_amostragem do
                    unidade.eventos_amostragem.each do |evento|
                      xml.evento_amostragem do
                        xml.horario_inicio(formatar_datetime(evento.horario_inicio))
                        xml.horario_fim(formatar_datetime(evento.horario_fim))
                        xml.esforco_real(evento.esforco_real)
                        escrever_valores_variaveis(xml, variaveis_por_nivel["evento"], valores_map, evento.id)

                        xml.registros_ocorrencia do
                          evento.registro_ocorrencias.each do |reg|
                            xml.registro_ocorrencia do
                              xml.data(reg.data)
                              xml.hora(reg.hora&.strftime("%H:%M:%S"))
                              xml.latitude(reg.latitude)
                              xml.longitude(reg.longitude)
                              xml.qtde_individuos(reg.qtde_individuos)
                              xml.ausencia_especie(reg.ausencia_especie)
                              xml.especie_nome_popular(reg.especie&.nome_popular)
                              escrever_valores_variaveis(xml, variaveis_por_nivel["registro"], valores_map, reg.id)
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
            end
          end
        end
      end
    end

    xml.target!
  end

  def gerar_csv(estudo:, agrupamento:)
    case agrupamento
    when "registro_ocorrencia" then gerar_por_registro(estudo)
    when "evento_amostragem"   then gerar_por_evento(estudo)
    when "unidade_amostral"    then gerar_por_unidade(estudo)
    when "campanha"            then gerar_por_campanha(estudo)
    when "especie"             then gerar_por_especie(estudo)
    end
  end

  private

  def gerar_por_registro(estudo)
    registros = RegistroOcorrencia
      .joins(evento_amostragem: { unidade_amostral: { campanha: :estudo } })
      .joins(:especie)
      .where(estudos: { id: estudo.id })
      .includes(:especie, evento_amostragem: { unidade_amostral: :campanha })
      .order("campanhas.nome, unidades_amostrais.nome, eventos_amostragem.horario_inicio, registro_ocorrencias.data, registro_ocorrencias.hora")

    variaveis = carregar_variaveis(estudo)
    valores_map = carregar_valores(estudo)

    colunas_fixas = %w[
      estudo_nome
      campanha_nome campanha_data_inicio campanha_data_fim campanha_descricao
      unidade_amostral_nome unidade_amostral_latitude unidade_amostral_longitude
      unidade_amostral_raio unidade_amostral_metodo_coleta unidade_amostral_esforco_amostral
      evento_horario_inicio evento_horario_fim evento_esforco_real
      registro_data registro_hora registro_latitude registro_longitude
      registro_qtde_individuos registro_ausencia_especie
      especie_classe especie_ordem especie_familia especie_genero
      especie_especie especie_nome_popular especie_status_conservacao especie_endemismo
    ]

    colunas_variaveis = variaveis.map { |v| "var_#{v.nivel_aplicacao}_#{v.nome}" }

    CSV.generate(col_sep: ";") do |csv|
      csv << colunas_fixas + colunas_variaveis

      registros.each do |reg|
        evento = reg.evento_amostragem
        unidade = evento.unidade_amostral
        campanha = unidade.campanha
        especie = reg.especie

        linha = [
          estudo.nome,
          campanha.nome, campanha.data_inicio, campanha.data_fim, campanha.descricao,
          unidade.nome, unidade.latitude, unidade.longitude,
          unidade.raio, unidade.metodo_coleta, unidade.esforco_amostral,
          formatar_datetime(evento.horario_inicio), formatar_datetime(evento.horario_fim), evento.esforco_real,
          reg.data, reg.hora&.strftime("%H:%M:%S"), reg.latitude, reg.longitude,
          reg.qtde_individuos, reg.ausencia_especie,
          especie.classe, especie.ordem, especie.familia, especie.genero,
          especie.especie, especie.nome_popular, especie.status_conservacao, especie.endemismo
        ]

        variaveis.each do |v|
          entidade_id = id_para_nivel(v.nivel_aplicacao, campanha, unidade, evento, reg)
          linha << valores_map.dig(v.id, entidade_id)
        end

        csv << linha
      end
    end
  end

  def gerar_por_evento(estudo)
    eventos = EventoAmostragem
      .joins(unidade_amostral: { campanha: :estudo })
      .where(estudos: { id: estudo.id })
      .includes(unidade_amostral: :campanha)
      .order("campanhas.nome, unidades_amostrais.nome, eventos_amostragem.horario_inicio")

    variaveis = carregar_variaveis(estudo).select { |v| %w[campanha unidade evento].include?(v.nivel_aplicacao) }
    valores_map = carregar_valores(estudo)

    colunas_fixas = %w[
      estudo_nome
      campanha_nome campanha_data_inicio campanha_data_fim
      unidade_amostral_nome unidade_amostral_latitude unidade_amostral_longitude
      evento_horario_inicio evento_horario_fim evento_esforco_real
      qtde_registros qtde_especies total_individuos
    ]

    colunas_variaveis = variaveis.map { |v| "var_#{v.nivel_aplicacao}_#{v.nome}" }

    agregados = RegistroOcorrencia
      .joins(evento_amostragem: { unidade_amostral: { campanha: :estudo } })
      .where(estudos: { id: estudo.id })
      .group(:evento_amostragem_id)
      .pluck(
        :evento_amostragem_id,
        Arel.sql("COUNT(*)"),
        Arel.sql("COUNT(DISTINCT especie_id)"),
        Arel.sql("COALESCE(SUM(qtde_individuos), 0)"),
      )
      .each_with_object({}) do |(evento_id, qtde, especies, individuos), hash|
        hash[evento_id] = { qtde: qtde, especies: especies, individuos: individuos }
      end

    CSV.generate(col_sep: ";") do |csv|
      csv << colunas_fixas + colunas_variaveis

      eventos.each do |evento|
        unidade = evento.unidade_amostral
        campanha = unidade.campanha
        agg = agregados[evento.id] || { qtde: 0, especies: 0, individuos: 0 }

        linha = [
          estudo.nome,
          campanha.nome, campanha.data_inicio, campanha.data_fim,
          unidade.nome, unidade.latitude, unidade.longitude,
          formatar_datetime(evento.horario_inicio), formatar_datetime(evento.horario_fim), evento.esforco_real,
          agg[:qtde], agg[:especies], agg[:individuos]
        ]

        variaveis.each do |v|
          entidade_id = id_para_nivel(v.nivel_aplicacao, campanha, unidade, evento, nil)
          linha << valores_map.dig(v.id, entidade_id)
        end

        csv << linha
      end
    end
  end

  def gerar_por_unidade(estudo)
    unidades = UnidadeAmostral
      .joins(campanha: :estudo)
      .where(estudos: { id: estudo.id })
      .includes(:campanha)
      .order("campanhas.nome, unidades_amostrais.nome")

    variaveis = carregar_variaveis(estudo).select { |v| %w[campanha unidade].include?(v.nivel_aplicacao) }
    valores_map = carregar_valores(estudo)

    colunas_fixas = %w[
      estudo_nome
      campanha_nome campanha_data_inicio campanha_data_fim
      unidade_amostral_nome unidade_amostral_latitude unidade_amostral_longitude
      unidade_amostral_raio unidade_amostral_metodo_coleta unidade_amostral_esforco_amostral
      qtde_eventos qtde_registros qtde_especies total_individuos
    ]

    colunas_variaveis = variaveis.map { |v| "var_#{v.nivel_aplicacao}_#{v.nome}" }

    agregados_evento = EventoAmostragem
      .joins(unidade_amostral: { campanha: :estudo })
      .where(estudos: { id: estudo.id })
      .group(:unidade_amostral_id)
      .count

    agregados_registro = RegistroOcorrencia
      .joins(evento_amostragem: { unidade_amostral: { campanha: :estudo } })
      .where(estudos: { id: estudo.id })
      .group("unidades_amostrais.id")
      .pluck(
        Arel.sql("unidades_amostrais.id"),
        Arel.sql("COUNT(*)"),
        Arel.sql("COUNT(DISTINCT especie_id)"),
        Arel.sql("COALESCE(SUM(qtde_individuos), 0)"),
      )
      .each_with_object({}) do |(unidade_id, qtde, especies, individuos), hash|
        hash[unidade_id] = { qtde: qtde, especies: especies, individuos: individuos }
      end

    CSV.generate(col_sep: ";") do |csv|
      csv << colunas_fixas + colunas_variaveis

      unidades.each do |unidade|
        campanha = unidade.campanha
        agg_reg = agregados_registro[unidade.id] || { qtde: 0, especies: 0, individuos: 0 }

        linha = [
          estudo.nome,
          campanha.nome, campanha.data_inicio, campanha.data_fim,
          unidade.nome, unidade.latitude, unidade.longitude,
          unidade.raio, unidade.metodo_coleta, unidade.esforco_amostral,
          agregados_evento[unidade.id] || 0, agg_reg[:qtde], agg_reg[:especies], agg_reg[:individuos]
        ]

        variaveis.each do |v|
          entidade_id = id_para_nivel(v.nivel_aplicacao, campanha, unidade, nil, nil)
          linha << valores_map.dig(v.id, entidade_id)
        end

        csv << linha
      end
    end
  end

  def gerar_por_campanha(estudo)
    campanhas = estudo.campanhas.order(:nome)

    variaveis = carregar_variaveis(estudo).select { |v| v.nivel_aplicacao == "campanha" }
    valores_map = carregar_valores(estudo)

    colunas_fixas = %w[
      estudo_nome
      campanha_nome campanha_data_inicio campanha_data_fim campanha_descricao
      qtde_unidades qtde_eventos qtde_registros qtde_especies total_individuos
    ]

    colunas_variaveis = variaveis.map { |v| "var_campanha_#{v.nome}" }

    agregados_unidade = UnidadeAmostral
      .joins(campanha: :estudo)
      .where(estudos: { id: estudo.id })
      .group(:campanha_id)
      .count

    agregados_evento = EventoAmostragem
      .joins(unidade_amostral: { campanha: :estudo })
      .where(estudos: { id: estudo.id })
      .group("campanhas.id")
      .count

    agregados_registro = RegistroOcorrencia
      .joins(evento_amostragem: { unidade_amostral: { campanha: :estudo } })
      .where(estudos: { id: estudo.id })
      .group("campanhas.id")
      .pluck(
        Arel.sql("campanhas.id"),
        Arel.sql("COUNT(*)"),
        Arel.sql("COUNT(DISTINCT especie_id)"),
        Arel.sql("COALESCE(SUM(qtde_individuos), 0)"),
      )
      .each_with_object({}) do |(campanha_id, qtde, especies, individuos), hash|
        hash[campanha_id] = { qtde: qtde, especies: especies, individuos: individuos }
      end

    CSV.generate(col_sep: ";") do |csv|
      csv << colunas_fixas + colunas_variaveis

      campanhas.each do |campanha|
        agg_reg = agregados_registro[campanha.id] || { qtde: 0, especies: 0, individuos: 0 }

        linha = [
          estudo.nome,
          campanha.nome, campanha.data_inicio, campanha.data_fim, campanha.descricao,
          agregados_unidade[campanha.id] || 0,
          agregados_evento[campanha.id] || 0,
          agg_reg[:qtde], agg_reg[:especies], agg_reg[:individuos]
        ]

        variaveis.each do |v|
          linha << valores_map.dig(v.id, campanha.id)
        end

        csv << linha
      end
    end
  end

  def gerar_por_especie(estudo)
    especies = Especie.where(estudo_id: estudo.id).order(:nome_popular)

    colunas_fixas = %w[
      estudo_nome
      especie_classe especie_ordem especie_familia especie_genero
      especie_especie especie_nome_popular especie_status_conservacao especie_endemismo
      qtde_registros total_individuos qtde_campanhas qtde_unidades
    ]

    agregados = RegistroOcorrencia
      .joins(:especie, evento_amostragem: { unidade_amostral: { campanha: :estudo } })
      .where(estudos: { id: estudo.id })
      .group(:especie_id)
      .pluck(
        :especie_id,
        Arel.sql("COUNT(*)"),
        Arel.sql("COALESCE(SUM(qtde_individuos), 0)"),
        Arel.sql("COUNT(DISTINCT campanhas.id)"),
        Arel.sql("COUNT(DISTINCT unidades_amostrais.id)"),
      )
      .each_with_object({}) do |(especie_id, qtde, individuos, campanhas, unidades), hash|
        hash[especie_id] = { qtde: qtde, individuos: individuos, campanhas: campanhas, unidades: unidades }
      end

    CSV.generate(col_sep: ";") do |csv|
      csv << colunas_fixas

      especies.each do |especie|
        agg = agregados[especie.id] || { qtde: 0, individuos: 0, campanhas: 0, unidades: 0 }

        csv << [
          estudo.nome,
          especie.classe, especie.ordem, especie.familia, especie.genero,
          especie.especie, especie.nome_popular, especie.status_conservacao, especie.endemismo,
          agg[:qtde], agg[:individuos], agg[:campanhas], agg[:unidades]
        ]
      end
    end
  end

  def carregar_variaveis(estudo)
    estudo.variaveis.order(:nivel_aplicacao, :nome).to_a
  end

  def carregar_valores(estudo)
    ValorVariavel
      .joins(:variavel)
      .where(variaveis: { estudo_id: estudo.id })
      .pluck(:variavel_id, :id_nivel_aplicacao, :valor)
      .each_with_object({}) do |(var_id, entidade_id, valor), hash|
        hash[var_id] ||= {}
        hash[var_id][entidade_id] = valor
      end
  end

  def id_para_nivel(nivel, campanha, unidade, evento, registro)
    case nivel
    when "campanha"  then campanha&.id
    when "unidade"   then unidade&.id
    when "evento"    then evento&.id
    when "registro"  then registro&.id
    end
  end

  def formatar_datetime(dt)
    dt&.strftime("%Y-%m-%d %H:%M")
  end

  def escrever_valores_variaveis(xml, variaveis_nivel, valores_map, entidade_id)
    return if variaveis_nivel.blank?

    xml.valores_variaveis do
      variaveis_nivel.each do |v|
        xml.valor do
          xml.variavel(v.nome)
          xml.valor(valores_map.dig(v.id, entidade_id))
        end
      end
    end
  end
end
